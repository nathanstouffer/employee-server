package webserver

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
enum class Position { ENGINEER, INTERN }

@Serializable
data class Employee(val id: Int, var last: String, var first: String, var pos: Position,
                    var age: Int, var salary: Int, var senior: Boolean) {
    init {
        if (senior && pos == Position.INTERN) { println("invalid employee setting: senior intern") }
    }

    // COMPANION OBJECT

    companion object {
        fun decode(str: String) = Json.decodeFromString<Employee>(str)
    }

    // PUBLIC METHODS

    fun encode() = Json.encodeToString(this)

    // STRING METHODS

    fun toFileString(): String {
        var ret = "${last},${first},"
        ret += when(pos) {
            Position.ENGINEER -> "Engineer"
            Position.INTERN -> "Intern"
        }
        ret += ",${age},${salary},${senior}"
        return ret
    }

    fun toVerboseString(): String {
        var ret = "-------------------------"
        ret += "\nid:     $id"
        ret += "\nfirst:  $first"
        ret += "\nlast:   $last"
        ret += "\npos:    $pos"
        ret += "\nage:    $age"
        ret += "\nsalary: $salary"
        ret += "\nsenior: $senior"
        ret += "\n-------------------------"
        return ret
    }

    override fun toString(): String {
        var ret = ""
        ret += "id: $id".padEnd(8, ' ')
        ret += "name: $last, $first".padEnd(30, ' ')
        ret += "pos: $pos".padEnd(16, ' ')
        ret += "age: $age".padEnd(10, ' ')
        ret += "salary: $salary".padEnd(17, ' ')
        ret += "senior: $senior"
        return ret
    }

}