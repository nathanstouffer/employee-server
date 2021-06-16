package webserver

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    runBlocking {
        header("starting client")
        val client = HttpClient(Java) {
            engine {
                // this: JavaHttpConfig
                threadsCount = 8
                pipelining = true
            }
        }

        val base = "http://localhost:8081"
        var run = true
        while (run) {
            // get the entry
            val option = printMainMenu()
            if (option == "") {
                continue
            }
            if (option == "q") {
                run = false
                break
            }
            if (option == "a") {
                val response = client.get<String>("$base/employees")
                println("Received -----")
                val emps = Json.decodeFromString<MutableList<Employee>>(response)
                for (emp in emps) { println(emp) }
            }
            else {
                println("Querying server for employee: $option")
                val response = client.get<String>("$base/employees/$option")
                val emp = Employee.decode(response)
                println("Received ----- $emp")

                // edit the entry
                val edit = editResp()
                if (edit == "y") {
                    val edit_opt = printEditMenu()
                    print("Value: ")
                    when (edit_opt) {
                        "1" -> emp.last = (readLine() ?: "").trim()
                        "2" -> emp.first = (readLine() ?: "").trim()
                        "3" -> emp.age = (readLine() ?: "0").trim().toInt()
                        "4" -> emp.salary = (readLine() ?: "0").trim().toInt()
                        else -> println("arrived at unreachable location")
                    }
                    val resp: HttpResponse = client.put("$base/employees/$option") { body = emp.encode() }
                    val updated_emp = Employee.decode(client.get<String>("$base/employees/$option"))
                    println("Updated ------ $updated_emp")
                }
            }
        }
    }
}

fun printMainMenu(): String {
    print("\nEnter employee number, a for all employees, or press q to quit: ")
    return inputResp()
}

fun editResp(): String {
    print("Edit (y/n): ")
    return inputResp()
}

fun printEditMenu(): String {
    println("1. last      2. first      3. age      4. salary")
    print("Option: ")
    return inputResp()
}

fun inputResp(): String {
    val resp = readLine()
    if (resp == null) {
        return ""
    }
    return resp.trim().lowercase()
}