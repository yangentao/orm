package dev.entao.utilapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.entao.kan.appbase.sql.GE

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Person.delete(Person::age GE 0)
        for (i in 1..9) {
            val p = Person()
            p.name = "Yang $i"
            p.age = i * 10
            p.insert()
        }

        val ls = Person.findAll(Person::age GE 60) {
            desc(Person::age)
        }
        for (p in ls) {
            println(p.toString())
        }
//        {"age":90,"id":279,"name":"Yang 9"}
//        {"age":80,"id":278,"name":"Yang 8"}
//        {"age":70,"id":277,"name":"Yang 7"}
//        {"age":60,"id":276,"name":"Yang 6"}
    }
}


