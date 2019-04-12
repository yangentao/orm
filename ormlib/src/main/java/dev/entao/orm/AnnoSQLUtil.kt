package dev.entao.orm

import dev.entao.base.nameClass
import dev.entao.base.nameProp
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField


val KClass<*>.nameClassSQL: String
    get() {
        return "`" + this.nameClass + "`"
    }


val KClass<*>.autoAlterTable: Boolean
    get() {
        return this.findAnnotation<AutoAlterTable>()?.value ?: true
    }


val KProperty<*>.fullNamePropSQL: String
    get() {
        var tabName = this.javaField?.declaringClass?.kotlin?.nameClass
        val fname = this.nameProp
        return "`" + tabName!! + "`.`" + fname + "`"
    }


val KProperty<*>.isExcluded: Boolean
    get() {
        return this.findAnnotation<Exclude>() != null
    }
val KProperty<*>.isPrimaryKey: Boolean
    get() {
        return this.findAnnotation<PrimaryKey>() != null
    }



