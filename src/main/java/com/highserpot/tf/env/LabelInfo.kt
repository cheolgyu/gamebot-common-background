package com.highserpot.tf.env

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object LabelInfo {

    val labels: JSONArray
    val labels_vector: Vector<String>
    val regex = mutableMapOf<Int, Regex>()

    init {
        val jsonObject = load()
        labels = jsonObject.getJSONArray("labels")
        val items = jsonObject.getJSONArray("regex")

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
          "regex":[
              "2=(.)*,2,(.)*",
              "3=(.)*,3,4,(.)*",
              "8=(.)*,7,8,(.)*",
              "0=0,(.)*,5,(.)*"
           ],
          "labels": [
            {
              "id": 0,
              "name": "100%",
              "min": 60,
              "action": "click",
              "screens": [
                "HUNTING_BOOK"
              ]
            },
            {
              "id": 1,
              "name": "창닫기",
              "min": 60,
              "action": "click",
              "screens": [
                "HUNTING_BOOK",
                "BASIC"
              ]
            },
            {
              "id": 2,
              "name": "걸어가기",              
              "min": 98,
              "action": "click",
              "screens": [
                "HUNTING_BOOK"
              ]
            },
            {
              "id": 3,
              "name": "완성하기",
              "min": 60,
              "action": "click",
              "screens": [
                "HUNTING_BOOK_COMPLETE"
              ]
            },
            {
              "id": 4,
              "name": "이동",
              "min": 95,
              "action": "click",
              "screens": [
                "HUNTING_BOOK"
              ]
            },
            {
              "id": 5,
              "name": "대기",
              "min": 98,
              "action": "click",
              "screens": [
                "HUNTING_BOOK"
              ]
            },
            {
              "id": 6,
              "name": "사냥",
              "min": 90,
              "action": "no_action",
              "screens": [
                "HUNTING_BOOK"
              ]
            },
            {
              "id": 7,
              "name": "이동-재클릭",
              "min": 98,
              "action": "no_action",
              "screens": [
                "HUNTING_BOOK"
              ]
            },
            {
              "id": 8,
              "name": "확인",
              "min": 98,
              "action": "click",
              "screens": [
                "HUNTING_BOOK"
              ]
            }
          ]
        }
    """.trimIndent()

        return JSONObject(jsonString)
    }

}