import org.hezistudio.dataBase.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File


fun main(){
    initDataBase()
}

fun initDataBase(){
    val dbFile = File("dataBase.db3")
    if (!dbFile.exists()) dbFile.createNewFile()
    val db = Database.connect("jdbc:sqlite:${dbFile.toURI()}","org.sqlite.JDBC")
    transaction {
//        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Groups, Users, Items,
            BlueMaps, BlueMapInputItems, BlueMapOutputItems,
            GroupMembers,GroupMemberBlueMaps,MemberItems,
            MemberSignIns
        )
        val user = User.new {
            userNum = -1L
        }
        val group = Group.new {
            number = -11L
            isWorking = false
        }
        Member.new {
            member = user
            this.group = group
            nickName = "张三"
        }
    }
    println(db.url)
}
