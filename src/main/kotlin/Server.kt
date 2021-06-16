package webserver

import io.ktor.application.*
import io.ktor.http.*
import java.io.File
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Job
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    // spin up the server
    val base = "http://localhost:8081/"
    header("starting server")
    embeddedServer(Netty, 8081) {
        routing {
            // get the number of employees
            get("/num_employees") {
                call.respondText(numEmployees().toString(), ContentType.Text.Plain)
                println("processed GET request for the number of employees")
            }
            // get a specific employee
            get("/employees/*") {
                val id = call.request.uri.split("/").last().toInt()
                call.respondText(readEmployee(id).encode(), ContentType.Text.Plain)
                println("processed GET request for employee $id")
            }
            // get all employees
            get("/employees") {
                call.respondText(Json.encodeToString(allEmployees()), ContentType.Text.Plain)
                println("processed GET request for all employees")
            }
            // edit an employee
            put("/employees/*") {
                val id = call.request.uri.split("/").last().toInt()
                val rcvd_str = call.receive<String>()
                val rcvd_emp = Employee.decode(rcvd_str)
                File("employees/$id").writeText(rcvd_emp.toFileString())
                call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
                println("processed PUT request editing employee $id")
            }
            // add a new employee
            post("/employees/create") {
                // record received employee
                val rcvd_str = call.receive<String>()
                val rcvd_emp = Employee.decode(rcvd_str)
                // create employee with the correct id
                val emp: Employee = with(rcvd_emp) {
                    Employee(nextId(), last, first, pos, age, salary, senior)
                }
                insertEmployee(emp)
                incrementNextId()
                call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
                println("processed POST request creating employee ${emp.id}")
            }
            // delete an employee
            delete("/employees/*") {
                val id = call.request.uri.split("/").last().toInt()
                deleteEmployee(id)
                call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
                println("processed DELETE request for employee $id")
            }
        }
    }.start(wait = true)
}

fun numEmployees(): Int {
    val line = File("employees/config").readLines()[2]
    val num = line.split(",")[1].toInt()
    return num
}

fun nextId(): Int {
    val line = File("employees/config").readLines()[2]
    val next = line.split(",")[1].toInt()
    return next
}

fun incrementNumEmployees() {
    val lines = File("employees/config").readLines()
    val numEmps = lines[1].split(",")[1].toInt() + 1
    val newLine = "num_employees,${numEmps}"
    File("employees/config").writeText("${lines[0]}\n${newLine}\n${lines[2]}")
}

fun incrementNextId() {
    val lines = File("employees/config").readLines()
    val nextId = lines[2].split(",")[1].toInt() + 1
    val newLine = "next_id,${nextId}"
    File("employees/config").writeText("${lines[0]}\n${lines[1]}\n${newLine}")
}

fun decrementNumEmployees() {
    val lines = File("employees/config").readLines()
    val numEmps = lines[1].split(",")[1].toInt() - 1
    val newLine = "num_employees,${numEmps}"
    File("employees/config").writeText("${lines[0]}\n${newLine}\n${lines[2]}")
}

fun insertEmployee(employee: Employee) {
    File("employees/${employee.id}").writeText(employee.toFileString())
    val name = "${employee.last}, ${employee.first}".lowercase()
    val ids = File("employees/sorted_ids").readLines()
    val new_ids = mutableListOf<Int>()
    // insert the employee into the correct place
    var inserted = false
    for (i in ids.indices) {
        val other = readEmployee(ids[i].toInt())
        if (!inserted && name < "${other.last}, ${other.first}".lowercase()) {
            new_ids.add(employee.id)
            inserted = true
        }
        new_ids.add(other.id)
    }
    if (!inserted) {
        new_ids.add(employee.id)
    }
    // write new ids to the file
    var output = ""
    for (id in new_ids) {
        output += id.toString() + "\n"
    }
    File("employees/sorted_ids").writeText(output)
    // increment the number of employees
    incrementNumEmployees()
}

fun deleteEmployee(id: Int) {
    // delete the file
    val file = Paths.get("employees/$id")
    Files.delete(file)
    // update the ids list
    val ids = File("employees/sorted_ids").readLines()
    val new_ids = mutableListOf<String>()
    // deletet the id
    for (i in ids.indices) {
        if (id != ids[i].toInt()) {
            new_ids.add(ids[i])
        }
    }
    // write new ids to the file
    var output = ""
    for (id in new_ids) {
        output += id + "\n"
    }
    File("employees/sorted_ids").writeText(output)
    // increment the number of employees
    decrementNumEmployees()
}

fun allEmployees(): List<Employee> {
    val lst = mutableListOf<Employee>()
    val ids = File("employees/sorted_ids").readLines()
    for (id in ids) {
        val employee = readEmployee(id.toInt())
        //println(employee)
        lst.add(employee)
    }
    return lst
}

fun readEmployee(id: Int): Employee {
    val split = File("employees/$id").readLines()[0].split(",")
    val employee = when (split[2]) {
        "Engineer" -> Employee(
            id,
            split[0],
            split[1],
            Position.ENGINEER,
            split[3].toInt(),
            split[4].toInt(),
            split[5].toBoolean()
        )
        "Intern" -> Employee(
            id,
            split[0],
            split[1],
            Position.INTERN,
            split[3].toInt(),
            split[4].toInt(),
            split[5].toBoolean()
        )
        else -> Employee(-1, "", "", Position.ENGINEER, -1, -1, false)
    }
    return employee
}

fun header(str: String): Unit {
    val pad = 55
    var output = "\n"
    output += " ${str.substring(0, str.length / 2)}".padStart(pad, '-')
    output += "${str.substring(str.length / 2)} ".padEnd(pad, '-')
    println(output)
}

fun dashes() {
    println("--------------------------------------------------------------------------------------------------------------")
}