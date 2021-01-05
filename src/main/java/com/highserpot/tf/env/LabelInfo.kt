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
          "regex":[],
          "labels": [
            {
              "id": 0,
              "name": "철광석",
              "min": 90,
              "action": "click",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 1,
              "name": "옥수수",
              "min": 90,
              "action": "click",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 2,
              "name": "나무",
              "min": 90,
              "action": "click",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 3,
              "name": "금화",
              "min": 90,
              "action": "click",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 4,
              "name": "건설&연구",
              "min": 90,
              "action": "no_action",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 5,
              "name": "병원",
              "min": 90,
              "action": "no_action",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 6,
              "name": "도움",
              "min": 90,
              "action": "click",
              "screens": [
                "BASIC"
              ]
            },
            {
              "id": 7,
              "name": "훈련",
              "min": 90,
              "action": "no_action",
              "screens": [
                "BASIC"
              ]
            }
          ]
        }
    """.trimIndent()

        return JSONObject(jsonString)
    }

}