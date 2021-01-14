package com.highserpot.tf.env

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object LabelInfo {

    val labels: JSONArray
    val labels_vector: Vector<String>
    val regex = mutableMapOf<Int, Regex>()
    val forced = mutableMapOf<Int, Int>()
    val forced_key : List<Int>
    val forced_value : List<Int>

    init {
        val jsonObject = load()
        labels = jsonObject.getJSONArray("labels")
        val items = jsonObject.getJSONArray("regex")
        val forced_arr = jsonObject.getJSONArray("forced")
        for (i in 0 until forced_arr.length()) {
            val aa = forced_arr.getString(i).split("->")
            forced.put(aa[0].toInt(), aa[1].toInt())
        }
        forced_key = forced.keys.toList()
        forced_value = forced.values.toList()

        for (i in 0 until items.length()) {
            val aa = items.getString(i).split("=")
            regex.put(aa[0].toInt(), aa[1].toRegex())
        }

        labels_vector = set_labels_vector()
    }

    fun set_labels_vector(): Vector<String> {
        var res: Vector<String> = Vector()

        for (i in 0 until labels.length()) {
            val item = labels.getJSONObject(i)
            res.add(item.get("id").toString())

        }


        return res
    }

    fun load(): JSONObject {
        val jsonString = """
       {
          "forced":[
            "11->0",
            "0->24",
            "24->25"
          ],
          "regex":[
              "11=(.)*,10,11,(.)*",
              "9=0,(.)*,8,9,(.)*",
              "9=(.)*,8,9,(.)*",
              "13=(.)*,12,13,14,(.)*",
              "13=(.)*,12,13,(.)*",
              "6=(.)*,6,7,(.)*",
              "19=(.)*,17,18,19,(.)*",
              "19=(.)*,18,19,(.)*",
              "22=(.)*,21,22,(.)*"
           ],
          "labels": [
            {
              "id": 0,
              "name": "홈",
              "min": 90,
              "action": "click",
              "screens": [
                "BAG_BASIC",
                "BAG_DISSASSEMBLE_ACTIVE",
                "BAG_DISSASSEMBLE_SELECT"
              ]
            },
            {
              "id": 1,
              "name": "절전풀",
              "min": 70,
              "action": "swipe",
              "screens": [
                "MODE_POWER_SAVE"
              ]
            },
            {
              "id": 2,
              "name": "기본풀",              
              "min": 90,
              "action": "click",
              "screens": [
                "MODE_BASIC"
              ]
            },
            {
              "id": 3,
              "name": "분해.시작",
              "min": 90,
              "action": "click",
              "screens": [
                "BAG_BASIC"
              ]
            },
            {
              "id": 4,
              "name": "일반",
              "min": 80,
              "action": "click",
              "screens": [
                "BAG_DISSASSEMBLE_ACTIVE_GENERAL"
              ]
            },
            {
              "id": 5,
              "name": "고급",
              "min": 70,
              "action": "click",
              "screens": [
                "BAG_DISSASSEMBLE_ACTIVE_ADVANCED"
              ]
            },
            {
              "id": 6,
              "name": "분해.선택",
              "min": 60,
              "action": "click",
              "screens": [
                "BAG_DISSASSEMBLE_SELECT"
              ]
            },
            {
              "id": 7,
              "name": "분해.선택.금화",
              "min": 90,
             "action": "no_action",
              "screens": [
                "BAG_DISSASSEMBLE_SELECT"
              ]
            },
            {
              "id": 8,
              "name": "분해.팝업창",
              "min": 90,
             "action": "no_action",
              "screens": [
                "BAG_DISSASSEMBLE_POPUP"
              ]
            },
            {
              "id": 9,
              "name": "분해.팝업창.확인",
              "min": 90,
              "action": "click",
              "screens": [
                "BAG_DISSASSEMBLE_POPUP"
              ]
            },
            {
              "id": 10,
              "name": "분해.결과창.금화",
              "min": 60,
             "action": "no_action",
              "screens": [
                "BAG_DISSASSEMBLE_RESULT"
              ]
            },
            {
              "id": 11,
              "name": "분해.결과.텍스트",
              "min": 60,
              "action": "click",
              "screens": [
                "BAG_DISSASSEMBLE_RESULT"
              ]
            },
            {
              "id": 12,
              "name": "스킵",
              "min": 90,
              "action": "click",
              "screens": [
                "SKIP"
              ]
            },
            {
              "id": 13,
              "name": "스킵.확인",
              "min": 90,
              "action": "click",
              "screens": [
                "SKIP_POPUP"
              ]
            },
            {
              "id": 14,
              "name": "스킵.취소",
              "min": 90,
             "action": "no_action",
              "screens": [
                "SKIP_POPUP"
              ]
            },
            {
              "id": 15,
              "name": "스마.off",
              "min": 90,
              "action": "click",
              "screens": [
                "MODE_BASIC_SMARTKEY_OFF"
              ]
            },
            {
              "id": 16,
              "name": "스마.on",
              "min": 90,
             "action": "no_action",
              "screens": [
                "MODE_BASIC_SMARTKEY_ON"
              ]
            },
            {
              "id": 17,
              "name": "팀교체.전체",
              "min": 80,
             "action": "no_action",
              "screens": [
                "UPDATE_TEAM"
              ]
            },
            {
              "id": 18,
              "name": "팀교체.중간",
              "min": 80,
             "action": "no_action",
              "screens": [
                "UPDATE_TEAM"
              ]
            },
            {
              "id": 19,
              "name": "팀교체.진행",
              "min": 90,
              "action": "click",
              "screens": [
                "UPDATE_TEAM"
              ]
            },
            {
              "id": 20,
              "name": "도움.전체",
              "min": 60,
             "action": "no_action",
              "screens": [
                "MINI_POPUP"
              ]
            },
            {
              "id": 21,
              "name": "도움.중간",
              "min": 60,
             "action": "no_action",
              "screens": [
                "MINI_POPUP"
              ]
            },
            {
              "id": 22,
              "name": "도움.닫기",
              "min": 90,
              "action": "click",
              "screens": [
                "MINI_POPUP"
              ]
            },
            {
              "id": 23,
              "name": "귀환",
              "min": 90,
             "action": "no_action",
              "screens": [
                "BAG_BASIC"
              ],
              "notify":{
                "use" : true,
                "txt" : "귀환!"
              }
            },
            {
              "id": 24,
              "name": "메뉴",
              "min": 60,
              "action": "click",
              "screens": [
                "MODE_BASIC","MODE_BASIC"
              ]
            },
            {
              "id": 25,
              "name": "절전",
              "min": 90,
              "action": "click",
              "screens": [
                "MODE_BASIC","MODE_BASIC"
              ]
            },
            {
              "id": 26,
              "name": "절전모드",
              "min": 90,
             "action": "no_action",
              "screens": [
                "MODE_POWER_SAVE"
              ]              
            },
            {
              "id": 27,
              "name": "방치중",
              "min": 90,
             "action": "no_action",
              "screens": [
                "MODE_POWER_SAVE"
              ]
            },
            {
              "id": 28,
              "name": "대기중",
              "min": 90,
             "action": "no_action",
              "screens": [
                "MODE_POWER_SAVE"
              ],
              "notify":{
                "use" : true,
                "txt" : "방치형필드 대기중 입니다."
              }
            }
          ]
        }
    """.trimIndent()

        return JSONObject(jsonString)
    }

}