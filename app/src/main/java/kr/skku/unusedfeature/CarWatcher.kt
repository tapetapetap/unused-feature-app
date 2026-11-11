package kr.skku.unusedfeature

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.MirrorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.vehicle.constant.control.DrivingModeType
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.control.WiperSwitch
import ai.pleos.playground.vehicle.listener.CruiseControlStateListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DrivingModeStateListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacAcStateListener
import ai.pleos.playground.vehicle.listener.HvacAutoStateListener
import ai.pleos.playground.vehicle.listener.HvacDefrosterStateListener
import ai.pleos.playground.vehicle.listener.HvacDualStateListener
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacMaxAcStateListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacSeatTemperatureListener
import ai.pleos.playground.vehicle.listener.HvacSeatVentilationListener
import ai.pleos.playground.vehicle.listener.HvacSideMirrorHeatStateListener
import ai.pleos.playground.vehicle.listener.HvacSteeringWheelHeatStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

private const val TAG = "CarWatcher"
private const val RETRY_MILLIS = 10_000L
private const val POLL_MILLIS = 3_000L

/** 차 연결 실패 알림 줄(contracts §5). 넣는 쪽과 지우는 쪽이 같은 값을 쓴다 */
val NO_SIGNAL = "차 신호를 받지 못하고 있어요 · ${RETRY_MILLIS / 1000}초마다 다시 연결해요"

private val DRIVER = SeatValue.ROW_1_LEFT
private val PASSENGER = SeatValue.ROW_1_RIGHT
private val REAR = listOf(SeatValue.ROW_2_LEFT, SeatValue.ROW_2_RIGHT)

/** SDK가 준 Area(좌석·창·미러) 가운데 [target]을 포함하는 가장 좁은 것. 없으면 null */
internal fun <A> pickArea(areas: Collection<A>, target: Any, values: (A) -> Array<*>): A? =
    areas.filter { target in values(it) }.minByOrNull { values(it).size }

/** 와이퍼 스위치 → Tracker가 쓰는 글자 */
internal fun wiperLabel(s: WiperSwitch): String = when {
    s == WiperSwitch.AUTO -> "AUTO"
    s == WiperSwitch.MIST -> "미스트"
    s == WiperSwitch.CONTINUOUS_LEVEL_1 -> "LO"
    s.name.startsWith("CONTINUOUS") -> "HI"
    s.name.startsWith("INTERMITTENT") -> "INT"
    else -> "OFF"
}

/**
 * Pleos Vehicle SDK 신호를 [tracker]에 반영하고 운전 시작·끝을 알린다.
 * - SDK 콜백은 백그라운드 스레드에서 오므로 모든 처리는 메인 스레드로 옮긴다.
 * - `register*`는 구독 직후 현재값을 한 번 보내 준다(에뮬레이터 로그로 확인). 그래서 `get*`으로 따로 읽지 않는다.
 * - 구독 함수가 없는 와이퍼 스위치는 운전 중 3초마다 읽는다(research R3).
 * - 사용 신호를 구독할 수 없는 기능은 [onUnsupported]로 알린다(원칙 II).
 */
class CarWatcher(
    context: Context,
    private val onChange: () -> Unit,
    private val onDriveEnd: (DriveStats) -> Unit,
    private val onProblem: (String) -> Unit,
    private val onUnsupported: (Feature) -> Unit,
) {
    private val vehicle = Vehicle(context.applicationContext)

    /** 앱 시계. 디버그(시연) 빌드는 10배 빠르다(research R5) */
    private val speed = if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) 10 else 1
    val tracker = Tracker { SystemClock.elapsedRealtime() * speed }

    private val main = Handler(Looper.getMainLooper())
    private val unregisters = mutableListOf<() -> Unit>() // 메인 스레드에서만 만진다
    private var stopped = false
    private var lastGear: VehicleGear? = null

    /** 기어가 P가 아님. 기록하지 않는 운전(운전 도중 시작)도 포함한다. 첫 기어 값 전에는 false */
    val notParked get() = lastGear != null && lastGear != VehicleGear.GEAR_PARK

    /** 5차: 화면용. 기어 값을 아직 모르면(연결 전·실패) 주행 중으로 본다. 주행 최적화 앱이라 Pleos가 대신 가려 주지 않는다 */
    val restricted get() = lastGear != VehicleGear.GEAR_PARK

    /** 5차: 후진 중에는 주행 중 권함을 미룬다 */
    val reverse get() = lastGear == VehicleGear.GEAR_REVERSE
    private var frontWiper: WindowArea? = null
    private val rearSeated = mutableMapOf<SeatValue, Boolean>() // 뒷좌석 자리별 값. 하나라도 앉았으면 앉은 것
    private val rearHeat = mutableMapOf<SeatValue, Int>()

    /** 스위치 위치(와이퍼·전조등·하이빔)는 구독 신호가 없어 운전 중 3초마다 읽는다 */
    private val poll = object : Runnable {
        override fun run() {
            if (stopped || !tracker.driving) return
            readSwitches()
            tracker.tick() // 5차: 비·운전 시간을 지금까지 값으로 두고 주행 중 권함을 판단한다(research R4·R5)
            onChange()
            main.postDelayed(this, POLL_MILLIS)
        }
    }

    fun start() {
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle 초기화 실패, ${RETRY_MILLIS / 1000}초 뒤 다시 시도", e)
            onProblem(NO_SIGNAL)
            main.postDelayed({ if (!stopped) start() }, RETRY_MILLIS) // 부팅 직후 차 서비스가 늦게 뜨는 경우
            return
        }
        onChange() // 연결됨: 신호가 하나도 오지 않아도 연결 실패 띠를 지운다(WatchService.onChange)
        // 차종에 따라 구독 호출 자체가 예외를 던질 수 있다. 하나가 실패해도 나머지는 지켜본다
        for ((what, watch) in listOf(
            "기어" to ::watchGear, "공조" to ::watchClimate, "시트" to ::watchSeats, "운전대" to ::watchWheel,
            "조명" to ::watchLights, "주행" to ::watchDriving, "와이퍼" to ::watchWipers, "미러" to ::watchMirror,
            "방향지시등" to ::watchTurnSignal,
        )) try { watch() } catch (e: Exception) { Log.w(TAG, "$what 구독 실패", e) }
        // 계기판 표시등(오토 홀드·LFA·i-PEDAL)은 읽기에도 내부 전용 권한(PRIVATE_CAR_SAFETY)이 필요해 쓰지 않는다
        main.postDelayed({ Log.i(TAG, "신호 받은 기능: ${tracker.stats.heard.sorted()}") }, 3_000)
    }

    fun stop() {
        stopped = true
        main.removeCallbacksAndMessages(null)
        unregisters.forEach { runCatching(it) }
        unregisters.clear()
        runCatching { vehicle.release() }
    }

    private fun ui(block: () -> Unit) {
        main.post { if (!stopped) block() }
    }

    private fun update(block: Tracker.() -> Unit) = ui { tracker.block(); onChange() }

    /** 기능 자체가 없는 차(capability 실패): 그 신호로 사용을 확인하는 [features]를 알린다 */
    private fun unsupported(what: String, vararg features: Feature): (Exception) -> Unit = { e ->
        Log.w(TAG, "$what 지원 안 함", e)
        ui { features.forEach(onUnsupported) }
    }

    /** 구독 중 일시 실패(예: 공조가 꺼져 온도를 못 읽음): 기록만 한다 */
    private fun transient(what: String): (Exception) -> Unit = { e -> Log.i(TAG, "$what 일시 실패: ${e.message}") }

    /**
     * capability 결과에서 [target] 좌석 Area를 골라 메인 스레드에서 구독한다. [subscribe]는 구독 해제 함수를 돌려준다.
     * 그 좌석이 없으면 [missing] 기능을 지원 안 함으로 알린다.
     */
    private fun onSeat(target: SeatValue, missing: Feature? = null, subscribe: (SeatArea) -> () -> Unit): (Collection<SeatArea>) -> Unit = { areas ->
        ui {
            val a = pickArea(areas, target) { it.values }
            if (a != null) unregisters += subscribe(a) else missing?.let(onUnsupported)
        }
    }

    private fun onGear(gear: VehicleGear?) = ui {
        if (gear == null || gear == VehicleGear.GEAR_UNKNOWN) return@ui
        val prev = lastGear
        lastGear = gear
        // 첫 값은 기준점. 운전 도중에 앱이 떴다면 그 운전은 세지 않는다(화면은 주행 중 화면, notParked).
        if (prev == gear) return@ui
        val parked = gear == VehicleGear.GEAR_PARK
        if (prev == VehicleGear.GEAR_PARK && !parked) {
            tracker.startDrive()
            main.post(poll)
        } else if (parked && tracker.driving) {
            main.removeCallbacks(poll)
            onDriveEnd(tracker.endDrive())
        }
        onChange()
    }

    private fun watchGear() {
        val gear = vehicle.getGear()
        val l = object : GearSelectionListener {
            override fun onSelected(g: VehicleGear?) = onGear(g)
            override fun onFailed(e: Exception) = transient("기어")(e)
        }
        gear.registerGearSelection(l)
        unregisters += { gear.unregisterGearSelection(l) }
    }

    private fun watchClimate() {
        val hvac = vehicle.getHVAC()

        hvac.checkHvacAutoCapabilities(onSeat(DRIVER) { a ->
            val l = object : HvacAutoStateListener {
                override fun onHvacAutoStateUpdated(v: Boolean?) { v?.let { update { auto(it) } } }
                override fun onFailed(e: Exception) = transient("공조 AUTO")(e)
            }
            hvac.registerHvacAutoState(a, l)
            ({ hvac.unregisterHvacAutoState(a, l) })
        }, unsupported("공조 AUTO", Feature.AUTO_CLIMATE))

        hvac.checkHvacDualCapabilities(onSeat(DRIVER) { a ->
            val l = object : HvacDualStateListener {
                override fun onHvacDualStateUpdated(v: Boolean?) { v?.let { update { dual(it) } } }
                override fun onFailed(e: Exception) = transient("DUAL")(e)
            }
            hvac.registerHvacDualState(a, l)
            ({ hvac.unregisterHvacDualState(a, l) })
        }, unsupported("DUAL", Feature.DUAL_CLIMATE))

        hvac.checkHvacAcCapabilities(onSeat(DRIVER) { a ->
            val l = object : HvacAcStateListener {
                override fun onHvacAcStateUpdated(v: Boolean?) { v?.let { update { ac(it) } } }
                override fun onFailed(e: Exception) = transient("에어컨")(e)
            }
            hvac.registerHvacAcState(a, l)
            ({ hvac.unregisterHvacAcState(a, l) })
        }, unsupported("에어컨"))

        hvac.checkHvacMaxAcCapabilities(onSeat(DRIVER) { a ->
            val l = object : HvacMaxAcStateListener {
                override fun onHvacMaxAcStateUpdated(v: Boolean?) { v?.let { update { maxAc(it) } } }
                override fun onFailed(e: Exception) = transient("MAX A/C")(e)
            }
            hvac.registerHvacMaxAcState(a, l)
            ({ hvac.unregisterHvacMaxAcState(a, l) })
        }, unsupported("MAX A/C", Feature.MAX_AC))

        hvac.checkHvacRecirculationCapabilities(onSeat(DRIVER) { a ->
            val l = object : HvacRecirculationStateListener {
                override fun onHvacAutoRecirculationStateUpdated(v: Boolean?) { v?.let { update { recirc(it) } } }
                override fun onFailed(e: Exception) = transient("내기 순환")(e)
            }
            hvac.registerHvacRecirculationState(a, l)
            ({ hvac.unregisterHvacRecirculationState(a, l) })
        }, unsupported("내기 순환"))

        hvac.checkHvacAutoRecirculationCapabilities(onSeat(DRIVER) { a ->
            val l = object : HvacRecirculationStateListener {
                override fun onHvacAutoRecirculationStateUpdated(v: Boolean?) { v?.let { update { autoRecirc(it) } } }
                override fun onFailed(e: Exception) = transient("오토 내/외기")(e)
            }
            hvac.registerHvacAutoRecirculationState(a, l)
            ({ hvac.unregisterHvacAutoRecirculationState(a, l) })
        }, unsupported("오토 내/외기", Feature.AUTO_RECIRC))

        hvac.checkHvacDefrosterCapabilities({ areas: List<WindowArea> ->
            ui {
                for ((window, feature) in listOf(WindowValue.FRONT_WINDSHIELD to Feature.FRONT_DEFROST, WindowValue.REAR_WINDSHIELD to Feature.REAR_DEFROST)) {
                    val a = pickArea(areas, window) { it.values }
                    if (a == null) { onUnsupported(feature); continue }
                    val l = object : HvacDefrosterStateListener {
                        override fun onHvacDefrosterStateUpdated(v: Boolean?) {
                            v?.let { update { if (feature == Feature.FRONT_DEFROST) frontDefrost(it) else rearDefrost(it) } }
                        }
                        override fun onFailed(e: Exception) = transient("제상")(e)
                    }
                    hvac.registerHvacDefrosterState(a, l)
                    unregisters += { hvac.unregisterHvacDefrosterState(a, l) }
                }
            }
        }, unsupported("제상", Feature.FRONT_DEFROST, Feature.REAR_DEFROST))

        hvac.checkHvacFanSpeedCapabilities({ areas: Map<SeatArea, *> ->
            onSeat(DRIVER) { a ->
                val l = object : HvacFanSpeedListener {
                    override fun onHvacFanSpeedUpdated(v: Int?) { v?.let { update { fan(it) } } }
                    override fun onFailed(e: Exception) = transient("풍량")(e)
                }
                hvac.registerHvacFanSpeed(a, l)
                ({ hvac.unregisterHvacFanSpeed(a, l) })
            }(areas.keys)
        }, unsupported("풍량"))

        hvac.checkHvacTemperatureCapabilities({ areas: Map<SeatArea, *> ->
            onSeat(DRIVER) { a ->
                val l = object : HvacTemperatureListener {
                    override fun onHvacTemperatureUpdated(v: Float?) { v?.let { update { temp(it) } } }
                    override fun onFailed(e: Exception) = transient("설정 온도")(e)
                }
                hvac.registerHvacTemperature(a, l)
                ({ hvac.unregisterHvacTemperature(a, l) })
            }(areas.keys)
        }, unsupported("설정 온도"))
    }

    private fun watchSeats() {
        val seat = vehicle.getSeat()
        fun occupancy(a: SeatArea, set: Tracker.(Boolean) -> Unit): () -> Unit {
            val l = object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(v: Boolean?) { v?.let { update { set(it) } } }
                override fun onFailed(e: Exception) = transient("착좌")(e)
            }
            seat.registerSeatOccupancy(a, l)
            return { seat.unregisterSeatOccupancy(a, l) }
        }
        fun temperature(a: SeatArea, set: Tracker.(Int) -> Unit): () -> Unit {
            val l = object : HvacSeatTemperatureListener {
                override fun onHvacSeatTemperatureUpdated(v: Int?) { v?.let { update { set(it) } } }
                override fun onFailed(e: Exception) = transient("시트 온도")(e)
            }
            seat.registerHvacSeatTemperature(a, l)
            return { seat.unregisterHvacSeatTemperature(a, l) }
        }

        seat.checkSeatOccupancyCapabilities({ areas: List<SeatArea> ->
            onSeat(PASSENGER) { a -> occupancy(a) { passenger(it) } }(areas)
            REAR.forEach { seat -> onSeat(seat) { a -> occupancy(a) { rearSeated[seat] = it; rearSeated(rearSeated.containsValue(true)) } }(areas) }
        }, unsupported("착좌"))

        seat.checkSeatTemperatureCapabilities({ areas: Map<SeatArea, *> ->
            onSeat(DRIVER, Feature.SEAT_HEAT) { a -> temperature(a) { seatHeat(it) } }(areas.keys)
            onSeat(PASSENGER, Feature.PASSENGER_SEAT) { a -> temperature(a) { passengerSeat(it) } }(areas.keys)
            REAR.forEach { seat -> onSeat(seat) { a -> temperature(a) { rearHeat[seat] = it; rearSeat(rearHeat.values.max()) } }(areas.keys) }
            if (REAR.none { r -> pickArea(areas.keys, r) { it.values } != null }) ui { onUnsupported(Feature.REAR_SEAT_HEAT) }
        }, unsupported("시트 열선", Feature.SEAT_HEAT, Feature.PASSENGER_SEAT, Feature.REAR_SEAT_HEAT))

        seat.checkSeatVentilationCapabilities({ areas: Map<SeatArea, *> ->
            onSeat(DRIVER) { a ->
                val l = object : HvacSeatVentilationListener {
                    override fun onHvacSeatVentilationUpdated(v: Int?) { v?.let { update { vent(it) } } }
                    override fun onFailed(e: Exception) = transient("통풍 시트")(e)
                }
                seat.registerHvacSeatVentilation(a, l)
                ({ seat.unregisterHvacSeatVentilation(a, l) })
            }(areas.keys)
        }, unsupported("통풍 시트")) // 시트 온도 음수로도 통풍을 알 수 있어 기능을 빼지 않는다
    }

    private fun watchWheel() {
        val wheel = vehicle.getSteeringWheel()
        wheel.checkHvacSteeringWheelHeatCapabilities({ _: AreaBounds<Int> ->
            ui {
                val l = object : HvacSteeringWheelHeatStateListener {
                    override fun onHvacSteeringWheelHeatStateUpdated(v: Int?) { v?.let { update { wheelHeat(it) } } }
                    override fun onFailed(e: Exception) = transient("운전대 열선")(e)
                }
                wheel.registerHvacSteeringWheelHeatState(l)
                unregisters += { wheel.unregisterHvacSteeringWheelHeatState(l) }
            }
        }, unsupported("운전대 열선", Feature.WHEEL_HEAT))
    }

    private fun watchLights() {
        val light = vehicle.getLight()
        // 선택지에 AUTO가 없으면 그 차에 없는 기능이다(에뮬레이터: 둘 다 [OFF, ON]). 모르면 권하지 않는다(원칙 II)
        light.checkLowBeamLightSwitchCapabilities({ values: List<LightSwitch> ->
            Log.i(TAG, "전조등 스위치 선택지: $values")
            if (LightSwitch.AUTOMATIC !in values) ui { onUnsupported(Feature.AUTO_LIGHT) }
        }, unsupported("전조등 스위치", Feature.AUTO_LIGHT))
        light.checkHighBeamLightSwitchCapabilities({ values: List<LightSwitch> ->
            Log.i(TAG, "하이빔 스위치 선택지: $values")
            if (LightSwitch.AUTOMATIC !in values) ui { onUnsupported(Feature.HIGH_BEAM_ASSIST) }
        }, unsupported("하이빔 스위치", Feature.HIGH_BEAM_ASSIST))
        readLowSwitch()
        readHighSwitch()
        val low = object : LightStateListener {
            override fun onLightStateUpdated(v: LightState?) {
                v ?: return
                update { light(v == LightState.ON) } // 상태 변화는 들어온 순서대로 반영
                readLowSwitch()
            }
            override fun onFailed(e: Exception) = transient("전조등")(e)
        }
        light.registerLowBeamLightState(low)
        unregisters += { light.unregisterLowBeamLightState(low) }
        val high = object : LightStateListener {
            override fun onLightStateUpdated(v: LightState?) {
                v ?: return
                update { highBeam(v == LightState.ON) }
                readHighSwitch()
            }
            override fun onFailed(e: Exception) = transient("하이빔")(e)
        }
        light.registerHighBeamLightState(high)
        unregisters += { light.unregisterHighBeamLightState(high) }

        light.checkAmbientLightStateCapabilities({ _, areas: Map<SeatArea, *> ->
            ui {
                val a = pickArea(areas.keys, DRIVER) { it.values } ?: areas.keys.firstOrNull()
                if (a == null) { onUnsupported(Feature.AMBIENT); return@ui }
                val l = object : LightStateListener {
                    override fun onLightStateUpdated(v: LightState?) { v?.let { update { ambient(it == LightState.ON) } } }
                    override fun onFailed(e: Exception) = transient("앰비언트")(e)
                }
                light.registerAmbientLightStateListener(a, l)
                unregisters += { light.unregisterAmbientLightStateListener(a, l) }
            }
        }, unsupported("앰비언트", Feature.AMBIENT))
    }

    private fun watchDriving() {
        val mode = vehicle.getDrivingMode()
        mode.checkCruiseControlStateCapabilities({
            ui {
                val l = object : CruiseControlStateListener {
                    override fun onCruiseControlStateUpdated(v: CruiseControl?) {
                        // ENABLED는 "켜 두고 대기"라 에뮬레이터 기본값이기도 하다. 속도를 잡고 있는 ACTIVATED만 사용으로 본다
                        v?.let { update { cruise(it == CruiseControl.ACTIVATED) } }
                    }
                    override fun onFailed(e: Exception) = transient("크루즈")(e)
                }
                mode.registerCruiseControlState(l)
                unregisters += { mode.unregisterCruiseControlState(l) }
            }
        }, unsupported("크루즈", Feature.CRUISE))

        mode.checkDrivingModeStateCapabilities({
            ui {
                val l = object : DrivingModeStateListener {
                    override fun onDrivingModeStateUpdated(v: DrivingModeType?) { v?.let { update { driveMode(it.name) } } }
                    override fun onFailed(e: Exception) = transient("주행 모드")(e)
                }
                mode.registerDrivingModeState(l)
                unregisters += { mode.unregisterDrivingModeState(l) }
            }
        }, unsupported("주행 모드", Feature.DRIVE_MODE))

        val odometer = vehicle.getOdometer()
        val distance = object : DistanceDrivenListener {
            override fun onDistanceDrivenUpdated(v: Float?) { v?.let { update { distance(it) } } }
            override fun onFailed(e: Exception) = transient("주행거리")(e)
        }
        odometer.registerDistanceDriven(distance)
        unregisters += { odometer.unregisterDistanceDriven(distance) }
    }

    private fun watchWipers() {
        val wiper = vehicle.getWiper()
        wiper.checkWiperStateCapabilities({ areas: Map<WindowArea, *> ->
            ui {
                val a = pickArea(areas.keys, WindowValue.FRONT_WINDSHIELD) { it.values } ?: return@ui
                val l = object : WiperStateListener {
                    override fun onWiperStateUpdated(v: WiperState?) { v?.let { update { wiper(it == WiperState.ON) } } }
                    override fun onFailed(e: Exception) = transient("와이퍼")(e)
                }
                wiper.registerWiperState(a, l)
                unregisters += { wiper.unregisterWiperState(a, l) }
            }
        }, unsupported("와이퍼 동작"))
        wiper.checkWiperSwitchCapabilities({ areas: Map<WindowArea, *> ->
            ui {
                frontWiper = pickArea(areas.keys, WindowValue.FRONT_WINDSHIELD) { it.values }
                if (frontWiper == null) onUnsupported(Feature.AUTO_WIPER) else readWiperSwitch()
            }
        }, unsupported("와이퍼 스위치", Feature.AUTO_WIPER))
    }

    // 스위치 읽기. 전조등·하이빔 스위치는 켜짐·꺼짐이 바뀔 때도 읽어 다음 변화의 수동 여부 판단에 쓴다
    private fun readLowSwitch() = vehicle.getLight().getLowBeamLightSwitch(
        { sw -> sw?.let { update { lightSwitch(it == LightSwitch.AUTOMATIC) } } }, transient("전조등 스위치"),
    )

    private fun readHighSwitch() = vehicle.getLight().getHighBeamLightSwitch(
        { sw -> sw?.let { update { highBeamSwitch(it == LightSwitch.AUTOMATIC) } } }, transient("하이빔 스위치"),
    )


    private fun readSwitches() {
        readWiperSwitch()
        readLowSwitch()
        readHighSwitch()
    }

    private fun readWiperSwitch() {
        val a = frontWiper ?: return
        vehicle.getWiper().getCurrentWiperSwitch(a, { s -> s?.let { update { wiperSwitch(wiperLabel(it)) } } }, transient("와이퍼 스위치"))
    }

    /** 5차: 켜진 동안 주행 중 권함을 미룬다. 못 받으면 미루기 조건 없이 판단한다(research R3) */
    private fun watchTurnSignal() {
        val signal = vehicle.getTurnSignal()
        val l = object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(v: TurnSignalValue?) {
                v ?: return
                Log.i(TAG, "방향지시등 $v")
                update { signalOn = v != TurnSignalValue.OFF }
            }
            override fun onFailed(e: Exception) = transient("방향지시등")(e)
        }
        signal.registerTurnSignalState(l)
        unregisters += { signal.unregisterTurnSignalState(l) }
    }

    private fun watchMirror() {
        val mirror = vehicle.getSideMirror()
        mirror.checkHvacSideMirrorHeatCapabilities({ areas: Map<MirrorArea, *> ->
            ui {
                val a = areas.keys.firstOrNull()
                if (a == null) { onUnsupported(Feature.MIRROR_HEAT); return@ui }
                val l = object : HvacSideMirrorHeatStateListener {
                    override fun onHvacSideMirrorHeatStateUpdated(v: Int?) { v?.let { update { mirrorHeat(it) } } }
                    override fun onFailed(e: Exception) = transient("미러 열선")(e)
                }
                mirror.registerHvacSideMirrorHeatState(a, l)
                unregisters += { mirror.unregisterHvacSideMirrorHeatState(a, l) }
            }
        }, unsupported("미러 열선", Feature.MIRROR_HEAT))
    }
}
