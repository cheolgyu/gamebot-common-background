package com.highserpot.tf.store

import android.util.Log
import com.highserpot.background.service.BackgroundServiceMP.Companion.disassembly_counter
import com.highserpot.background.service.BackgroundServiceMP.Companion.lastProcessingTimeMs
import com.highserpot.tf.env.LabelInfo
import com.highserpot.tf.tflite.Classifier
import java.util.*
import kotlin.collections.ArrayList

/*
        ==============
        1. 클릭대상 선정은? ==> 클릭대상목록 만들기, 유효한 객체들
        인식시간 * 2 시간에 인식된것이 인식시간횟수 만큼 있다면 인식된 것으로 처리한다.
        인식시간 * 3 시간 안에 인식횟수가 인식시간횟수 만큼 있어야 인식된 것으로 처리한다.
        시간, lb를 만들어서 저장한다. 이전 목록을 저장한다.
        ==============
        2. 액션대상 선정은?
        -----------------
        2-1. 내가정한 액션대상 선정
        정규식을 통해 선정되면 번갈아 선정에서 제외한다.
        2-2. 번갈아 액션대상 선정
        내가 정한 액션대상에 선정되지 않으면 번갈아 선정한다.
        클릭대상이 선정되면 액션목록의  마지막 값에 따라 다른것을 액션으로 선정해야된다.
        2-3 한개의 화면에 같은 객체가 여러개 일떄
        한개의 화면에 같은 객체가 여러개일떄는 높이가 높은것이 우선적이다.
        ==============
        3. 강제 액션은?
        예) 분해결과 후에만 홈 클릭!
        강제액션이 활성되면 선정된 액션대상의 상태값이 존재한다면 강제액션목록에 저장한다.
        ---
        강제액션이 활성되면
        인식시간 * 3 시간의 강제액션 목록에 있다면 미리 만든 강제액션설정 변수에서
        해당 액션에 해당하는 객체id을 꺼내고 선정된 클릭대상 목록에서 객체id를 찾아서
        인식목록의 위치를 바꿔서 클릭되게 한다.
        ==============
         4. 지속적 액션은?
        인식시간이 1초 보다 적으면
        클릭대상이 선정이 되면 인식시간의 빠르기에 따라 지속적인 클릭이 되니깐
        인식시간이 1초 미만 이면서 인식시간*2의 액션목록에 같은 객체가 있으면 취소한다.
        ==============
        핸드폰에서 광고영역은?
        핸드폰에서 광고영역이 너무 크다. 애뮬과 비교해서 처리하기는 부정확하고
        대기모드와 실행모드로 만들자.
        대기모드는 배너이고 실행모드는 좀더큰 광고영역으로 하고 영역고정시키자.
    */
class ScreenHistory {

    // 인식화면의 속도
    var r_speed : Long = 0
    fun run(){

    }
    fun valid_list(){}
    fun select_target(){}
    fun forced_target(){}
    fun continuous_action(){}






    // 상태내역: 이전 상태에 따라 클릭해야될 경우
    var history_for_status: MutableMap<Long, ScreenStatus> = mutableMapOf()

    // 쉽게 말해 에임.
    // 첫번째 타겟내역: 클릭대상이 여러개일때 여러개를 모두 배열의 0번쨰에 위치하기 위해
    var history_for_aiming: MutableMap<Long, Int> = mutableMapOf()

    // 쉽게말해 에임된것을 액션
    // 클릭내역: 게임화면갱신속도 보다 인식속도 빨라 지속적클릭으로 예상흐름을 망칠경우
    // 토글 버튼이 있는경우에 사용. 토글 전 인식하여 클릭했지만 실제화면은 토글후 클릭이됨.
    var history_for_aiming_action: MutableMap<Long, Int> = mutableMapOf()

    // 분해카운터 내역
    var history_for_counter_disassembly: ArrayList<Long> = arrayListOf()

    // 조건 조회용 시간 초
    var time_for_confirm_consecutive_clicks: Long = 0
    var time_for_common_status: Long = 0

    // 히스토리 유지시간 초
    // 분해카운터내역 유지시간
    var time_for_keep_counter_disassembly: Long = 0
    // time_for_keep_history_for_status= 분해 전체 분해완료 후 메뉴+절전모드
    var time_for_keep_history_for_status: Long = 0
    var time_for_keep_history_for_aiming: Long = 0
    var time_for_keep_history_for_aiming_action: Long = 0

    fun set_time(in_time: Long) {
        val time = in_time * 2
        time_for_confirm_consecutive_clicks = time * 1
        time_for_common_status = time_for_confirm_consecutive_clicks * 1
        time_for_keep_history_for_status = time * 1
        time_for_keep_history_for_aiming = time_for_confirm_consecutive_clicks * 3
        time_for_keep_history_for_aiming_action = 0
        time_for_keep_counter_disassembly = time_for_confirm_consecutive_clicks * 4

    }

    val TAG = this.javaClass.name
    val USE_STATUS = true


    fun change_order(detections: ArrayList<Classifier.Recognition>): ArrayList<Classifier.Recognition> {
        Log.d(TAG, "================================")
        Log.d(
            TAG,
            "시간.time_for_confirm_consecutive_clicks         ${TAG}=${time_for_confirm_consecutive_clicks}"
        )
        Log.d(TAG, "시간.time_for_common_status               ${TAG}=${time_for_common_status}")
        Log.d(
            TAG,
            "시간.time_for_keep_history_for_status            ${TAG}=${time_for_keep_history_for_status}"
        )
        Log.d(
            TAG,
            "시간.time_for_keep_history_for_aiming            ${TAG}=${time_for_keep_history_for_aiming}"
        )
        Log.d(
            TAG,
            "시간.time_for_keep_history_for_aiming_action     ${TAG}=${time_for_keep_history_for_aiming_action}"
        )
        Log.d(
                TAG,
        "시간.time_for_keep_counter_disassembly               ${TAG}=${time_for_keep_counter_disassembly}"
        )
        val started: Long = System.currentTimeMillis()
        var res: ArrayList<Classifier.Recognition> = detections
        if (detections.isNotEmpty()) {
            val updateDetectList = pre_run(started, detections)
            if (!updateDetectList.update) {
                res = processing_run(started, detections)

            } else {
                res = updateDetectList.detections

            }
            Log.d(TAG, "0번쨰1=${res[0].lb.getString("action")}")
            history_for_aiming_action =
                history_for_aiming_action.filterKeys { it > started - time_for_keep_history_for_aiming_action } as MutableMap<Long, Int>
            res.forEachIndexed { index, it ->
                if ("no_action" != it.lb.getString("action")) {
                    var has_click = false
                    history_for_aiming_action.forEach { _, id ->
                        if (id == it.lb.getInt("id")) {
                            has_click = true
                        }
                    }
                    if (!has_click) {
                        if (!updateDetectList.update) {
                            it.click = it.lb.getJSONArray("screens").length() == 1
                        } else {
                            it.click = true
                        }
                    } else {
                        it.click = false
                    }
                }

            }
            Log.d(TAG, "0번쨰2=${res[0].lb.getString("action")},${res[0]}")
            if (!res[0].click && res.size > 1 && !updateDetectList.update) {
                Collections.swap(res, 0, res.lastIndex);
            }

            Log.d(TAG, "0번쨰3=${res[0].lb.getString("action")},${res[0]}")
            update_history(started, res[0], updateDetectList.update)
            Log.d(TAG, "0번쨰=${res[0]}")
            Log.d(TAG, "res =${res}")
        }

        return res

    }

    class UpdateDetectList {
        var update: Boolean = false
        lateinit var detections: ArrayList<Classifier.Recognition>

        constructor(update: Boolean, detections: ArrayList<Classifier.Recognition>) : this() {
            this.update = update
            this.detections = detections
        }

        constructor()
    }


    fun update_history(started: Long, item: Classifier.Recognition, is_status_history: Boolean) {
        Log.d(TAG, "run - update_history")
        if (item.click) {

            history_for_aiming_action.put(started, item.lb.getInt("id"))
        }

        if (USE_STATUS) {
            val cur_status =
                ScreenStatus.valueOf(item.lb.getJSONArray("screens").getString(0))
            history_for_status.put(started, cur_status)


            if (cur_status == ScreenStatus.HUNTING_BOOK_COMPLETE){
                if (history_for_counter_disassembly.size == 0){
                    disassembly_counter++
                }
                history_for_counter_disassembly.add(started)

            }
        }
        history_for_aiming.put(started, item.lb.getInt("id"))

        // 히스토리 업데이트
        history_for_counter_disassembly = history_for_counter_disassembly.filter { it > started - time_for_keep_counter_disassembly } as ArrayList<Long>
        history_for_aiming =
            history_for_aiming.filterKeys { it > started - time_for_keep_history_for_aiming } as MutableMap<Long, Int>
        history_for_aiming_action =
            history_for_aiming_action.filterKeys { it > started - time_for_keep_history_for_aiming_action } as MutableMap<Long, Int>
        history_for_status =
            history_for_status.filterKeys { it > started - time_for_keep_history_for_status } as MutableMap<Long, ScreenStatus>

        Log.d("history11", "=======================================================")
        Log.d("history11", "history_for_aiming=              ${history_for_aiming}")
        Log.d("history11", "history_for_aiming_action=       ${history_for_aiming_action}")
        Log.d("history11", "history_for_status=              ${history_for_status}")
        Log.d("history11", "history_for_counter_disassembly= ${history_for_counter_disassembly}")
    }

    // 여러개의 클릭 아이템중 클릭할 아이템 찾기
    fun toClick(detections: ArrayList<Classifier.Recognition>): ArrayList<Classifier.Recognition> {
        Log.d(TAG, "run - toClick")
        // 여러개의 클릭 아이템
        var id_string = ""
        for (i in 0 until LabelInfo.labels.length()) {
            if (detections.find { it.lb.getInt("id") == i } != null) {
                id_string += "$i,"
            } else {
                id_string += "$i-,"
            }
        }
        Log.d(TAG, "lb=${id_string}")

        // 정규식으로 클릭할 아이템 목록 만들기.
        var toClickList = arrayListOf<Classifier.Recognition>()
        for ((k, v) in LabelInfo.regex) {

            if (v.find(id_string) != null) {

                var rg = detections.find { it.lb.getInt("id") == k }!!
                detections.remove(rg)
                toClickList.add(rg)
                Log.d(TAG, "정규식-참=${rg.lb.getString("name")}")

            }
        }
        Log.d(TAG, "toClickList=${toClickList.toString()}")
        Log.d(TAG, "toClickList+toClickList=${toClickList.toString()}")



        //나머지는 클릭안되게 처리하기
        if(toClickList.size > 0){
            detections.forEach { it ->

                if ("no_action" != it.lb.getString("action") && it.lb.getJSONArray("screens").length()==1) {
                    //it.lb.put("action","no_action")
                }
            }
        }

        //클릭할 아이템목록 먼저오고 나머지
        toClickList.addAll(detections)

        return toClickList
    }

    //Confirm consecutive clicks
    fun confirm_consecutive_clicks(
        started: Long,
        detections: ArrayList<Classifier.Recognition>
    ): ArrayList<Classifier.Recognition> {
        Log.d(TAG, "run - confirm_consecutive_clicks")
        val history_list =
            history_for_aiming.filterKeys { it > started - time_for_confirm_consecutive_clicks }
                .toSortedMap(reverseOrder())
        var move_index = -1
        lateinit var item: Classifier.Recognition
        detections.forEachIndexed { index, json ->
            val d_id = json.lb.getInt("id")
            if ("no_action" != json.lb.getString("action")) {
                history_list.forEach { (_, id) ->
                    if (id == d_id) {
                        item = json
                        move_index = index
                    }
                }

            }

        }


        Log.d(TAG, "history_list        - ${history_list}")
        Log.d(TAG, "detections          - ${detections}")

        if (move_index > -1 && detections.size > 1 && detections.lastIndex != move_index) {
            Log.d(TAG, "맨뒤로 갈 대상은=?${item.lb.getString("name")}")
            Collections.swap(detections, move_index, detections.lastIndex);
        }
        Log.d(TAG, "detections after    - ${detections}")

        return detections
    }

    //processing
    fun processing_run(
        started: Long,
        detections: ArrayList<Classifier.Recognition>
    ): ArrayList<Classifier.Recognition> {
        Log.d(TAG, "run - processing_run")
        var toClickList = toClick(detections)

        var proc_list: ArrayList<Classifier.Recognition>
        if (toClickList.size > 0) {
            proc_list = toClickList
        } else {
            proc_list = detections
        }

        // 클릭용 과 서브용 분리하기
        var click = mutableListOf<Classifier.Recognition>()
        var sub = mutableListOf<Classifier.Recognition>()
        proc_list.forEach {
            if (USE_STATUS) {
                if (it.lb.getJSONArray("screens").length() > 1) {
                    sub.add(it)
                } else {
                    if ("no_action" != it.lb.getString("action")) {
                        click.add(it)
                    } else {
                        sub.add(it)
                    }
                }
            } else {
                if ("no_action" != it.lb.getString("action")) {
                    click.add(it)
                } else {
                    sub.add(it)
                }
            }

        }

        // 클릭용을 이전 클릭내역과 비교하기.
        var sort_click = confirm_consecutive_clicks(
            started,
            click as ArrayList<Classifier.Recognition>
        )
        sort_click.addAll(sub)
        return sort_click

    }

    // 공통아이템이 특정 화면에서만 공통아이템을 클릭해야될 경우
    // ex)홈버튼은 분해결과-텍스트가 나와야 클릭가능함.
    fun pre_run(
        started: Long,
        detections: ArrayList<Classifier.Recognition>
    ): UpdateDetectList {
        Log.d(TAG, "run - pre_run")
        var update = false
        var res = UpdateDetectList(update, detections)
        if (USE_STATUS) {
            val common_item: Classifier.Recognition? =
                detections.find { it.lb.getJSONArray("screens").length() > 1 }
            Log.d(TAG, "common_item=${common_item}")
            if (common_item != null) {
//                val get_history =
//                    history_for_status.filterKeys { it > started - time_for_common_status }
//                        .toSortedMap(reverseOrder())

                // 특정 화면체크
                history_for_status.forEach { (_, u) ->
                    if (u == ScreenStatus.HUNTING_BOOK_COMPLETE) {
                        res.detections.remove(common_item)
                        common_item?.click = true
                        res.detections.add(0, common_item!!)
                        res.update = true
                        Log.d(TAG, "분해카운터               ${TAG}=${disassembly_counter}")
                    }
                }
            }
        }

        return res
    }


}