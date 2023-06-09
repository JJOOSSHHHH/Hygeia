package com.hygeia.classes

data class DataProducts (
    val ID : String? = null,
    val Name : String? = null,
    val Price : String? = null,
    val Quantity : String? = null,
    val Slot : Int? = null,
    val Status : Int? = null,
    var Points : Double? = null,
    var Reward : Double? = null,
    var Count: Int = 0
)
