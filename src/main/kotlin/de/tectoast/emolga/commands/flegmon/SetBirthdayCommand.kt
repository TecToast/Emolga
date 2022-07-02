package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import de.tectoast.emolga.utils.sql.managers.BirthdayManager

class SetBirthdayCommand : PepeCommand("setbirthday", "Trägt deinen Geburtstag ein") {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("bday", "Geburtstag", "Der Geburtstag lol", ArgumentManagerTemplate.Text.any())
            .setExample("!setbirthday 30.01.2005").build()
    }

    override fun process(e: GuildCommandEvent) {
        val msg = e.message!!.contentDisplay
        val tco = e.textChannel
        val birthday = msg.split(" ".toRegex())[1]
        val bd = birthday.split("\\.".toRegex())
        if (bd.size != 3) {
            e.reply("Das ist kein valides Datum!")
            return
        }
        val day: Int = try {
            bd[0].toInt()
        } catch (ex: NumberFormatException) {
            e.reply("Das ist kein valider Tag!")
            return
        }
        val month: Int = try {
            bd[1].toInt()
        } catch (ex: NumberFormatException) {
            e.reply("Das ist kein valider Monat!")
            return
        }
        val year: Int = try {
            bd[2].toInt()
        } catch (ex: NumberFormatException) {
            e.reply("Das ist kein valides Jahr!")
            return
        }
        val uid = e.author.idLong
        /*if (Database.update("UPDATE birthdays SET year = " + year + ", month = " + month + ", day = " + day + " WHERE userid = " + uid) == 0) {
            Database.insertBuilder("birthdays", "userid, year, month, day", uid, year, month, day);
        }*/BirthdayManager.addOrUpdateBirthday(uid, year, month, day)
        tco.sendMessage(
            "Dein Geburtstag wurde erfolgreich auf den " + getWithZeros(day, 2) + "." + getWithZeros(
                month,
                2
            ) + "." + year + " gesetzt!"
        ).queue()
    }
}