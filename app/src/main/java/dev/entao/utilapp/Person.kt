package dev.entao.utilapp

import dev.entao.kan.json.YsonObject
import dev.entao.orm.AutoInc
import dev.entao.orm.Model
import dev.entao.orm.ModelClass
import dev.entao.orm.PrimaryKey

class Person(yo: YsonObject = YsonObject()) : Model(yo) {

    @AutoInc
    @PrimaryKey
    var id: Int by model


    var name: String by model

    var age: Int by model


    companion object : ModelClass<Person>()
}