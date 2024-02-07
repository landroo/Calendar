package org.landroo.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PathEffect
import android.util.Log
import java.time.DayOfWeek
import java.util.*
import kotlin.collections.ArrayList
import android.icu.util.ULocale.getCountry
import java.lang.Exception


class CalClass(context: Context) {

    private val TAG = CalClass::class.java.simpleName
    private val GESTURE_THRESHOLD_DIP = 16.0f
    val COLOR11 = 0x22665544.toInt()
    val COLOR12 =  0x22EEDD33.toInt()
    val COLOR13 = 0x22009955.toInt()
    val COLOR14 = 0x22FF6622.toInt()
    val COLOR15 = 0x22FF2233.toInt()
    val COLOR21 = 0xCC665544.toInt()
    val COLOR22 =  0xCCEEDD33.toInt()
    val COLOR23 = 0xCC009955.toInt()
    val COLOR24 = 0xCCFF6622.toInt()
    val COLOR25 = 0xCCFF2233.toInt()

    val WIDTH = 60.0
    val HEIGHT = 40.0

    val dayNames1 = arrayOf("Hétfő", "Kedd", "Szerda", "Csütörtök", "Péntek", "Szombat", "Vasárnap")
    val dayNames2 = arrayOf("H", "K", "Sz", "Cs", "P", "Sz", "V")
    val monthNames = arrayOf("Január", "Február", "Március", "Április", "Május", "Június", "Július", "Augusztus", "Szeptember", "Október", "November", "December")

    private val days: ArrayList<Day> = ArrayList()

    private val selectPaint: Paint = Paint()
    private val strokePaint: Paint = Paint()
    private val foreColor: Paint = Paint()
    private val backColor: Paint = Paint()

    private var effect: PathEffect? = null

    private var scale: Float = 1f
    private var width = WIDTH
    private var height = HEIGHT
    private var groupID = 0

    init {
        scale = context.resources.displayMetrics.density
        width *= scale
        height *= scale
        Log.i(TAG, "scale: " + scale)

        foreColor.textSize = GESTURE_THRESHOLD_DIP * scale + 0.5f

        strokePaint.color = 0xFF000000.toInt()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 5f

        selectPaint.color = 0x88fc038c.toInt()
        selectPaint.style = Paint.Style.FILL

        backColor.color = 0xFFAAAAAA.toInt()
        //backColor.style = Paint.Style.STROKE
    }

    fun drawDays(canvas: Canvas, xPos: Double, yPos: Double, zx: Double, zy: Double, displayWidth: Int, displayHeight: Int) {
        var paint: Paint
        for (item in days) {

            val dx = xPos + item.px * zx
            val dy = yPos + item.py * zy

            if (dx > -item.width * zx && dx < displayWidth + item.width * zx && dy > -item.height * zy && dy < displayHeight + item.height * zy) {

                canvas.save()
                canvas.translate((xPos + item.px * zx).toFloat(), (yPos + item.py * zy).toFloat())
                canvas.scale(zx.toFloat(), zy.toFloat())

                canvas.clipRect(0f, 0f, item.width.toFloat(), item.height.toFloat())

                when(item.mode) {
                    Day.SELECTED -> {
                        paint = selectPaint
                    }
                    else -> {
                        paint = backColor
                        paint.color = item.color
                        strokePaint.pathEffect = null
                    }
                }

                canvas.drawPath(item.path, paint)

                canvas.drawPath(item.path, strokePaint)

                if(item.mode == Day.SELECTED) {
                    strokePaint.pathEffect = effect
                    canvas.drawPath(item.bound, strokePaint)
                    if(item.mode == Day.SELECTED) {
                        canvas.drawRect(item.rect, paint)
                        strokePaint.pathEffect = null
                        canvas.drawRect(item.rect, strokePaint)
                    }
                }

                canvas.rotate(item.angle.toFloat())

                canvas.drawText("" + item.text, 10f, item.v.toFloat() + foreColor.textSize / 2 + 20f, foreColor)

                canvas.restore()
            }
        }
    }

    fun selectDate(px: Double, py: Double, zx: Double, zy: Double): Day? {
        var selItem: Day? = null
        for (i in days.size - 1 downTo 0) {
            val item = days.get(i)
            if (item.type != 0 && item.isInside(px, py, zx, zy)) {
                Log.i(TAG, "" + item.info + " " + item.type + " " + item.id + " " + item.descript)
                selItem = item
                break
            }
        }
        return selItem
    }

    fun addYear(type: Int) {
        if(type == 0) {
            for (i in 1..12) {
                addLineDays(i, (i - 1) * 7 * 40)
            }
        }
        else {
            var m = 1
            for (i in 1..4) {
                for (j in 1..3) {
                    addBlockDays(m++ , 40 + (j - 1) * 8 * WIDTH, 40 + (i - 1) * 9 * HEIGHT, type)
                }
            }
        }
    }

    fun addLineDays(month: Int, yOff: Int) {
        val dt = Date()
        val maxDays = arrayOf(31, if(dt.year % 4 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        var px = 40.0
        var py = 40.0 + yOff

        var newDay = Day(px, py, 0, scale, 0, "month", "", 0)
        newDay.setDay(px, py * scale, width * 3 + width * maxDays[month - 1], height, 0, "" + month + ". " + monthNames[month - 1], 0xFFFFFFFF.toInt())
        days.add(newDay)
        newDay = Day(px, py, 0, scale, 0, "line1", "", 0)
        newDay.setDay(px, py * scale + height * 1, width * 3 + width * maxDays[month - 1], height, 100, "Line1", COLOR11)
        days.add(newDay)
        newDay = Day(px, py, 0, scale, 0, "line2", "", 0)
        newDay.setDay(px, py * scale + height * 2, width * 3 + width * maxDays[month - 1], height, 200, "Line2", COLOR12)
        days.add(newDay)
        newDay = Day(px, py, 0, scale, 0, "line3", "", 0)
        newDay.setDay(px, py * scale + height * 3, width * 3 + width * maxDays[month - 1], height, 300, "Line3", COLOR13)
        days.add(newDay)
        newDay = Day(px, py, 0, scale, 0, "line4", "", 0)
        newDay.setDay(px, py * scale + height * 4, width * 3 + width * maxDays[month - 1], height, 400, "Line4", COLOR14)
        days.add(newDay)
        newDay = Day(px, py, 0, scale, 0, "line5", "", 0)
        newDay.setDay(px, py * scale + height * 5, width * 3 + width * maxDays[month - 1], height, 500, "Line5", COLOR15)
        days.add(newDay)

        px += width * 2

        var color = 0x00FFFFFF.toInt()
        for(i in 1..maxDays[month - 1]) {

            val date = Date(dt.year, month - 1, i)
            val cal = Calendar.getInstance()
            cal.time = date
            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            dayOfWeek -= 1
            if(dayOfWeek < 1) {
                dayOfWeek = 7
            }
            val info = "" + (dt.year + 1900) + "." + month + "." + i
            //Log.i(TAG, info)
            val newDay = Day(px + i * width, py, 0, scale, 0, info, "", 0)
            color = 0x00FFFFFF.toInt()
            if (dayOfWeek == 6 || dayOfWeek == 7)
                color = 0x88FFFFFF.toInt()
            val toDay = "" + (dt.year + 1900) + "." + (dt.month + 1) + "." + dt.date
            if(info == toDay)
                color = 0x88216996.toInt()
            newDay.setDay(px + i * width, py * scale, width, height * 6, i,
                i.toString() + ". " + dayNames2[dayOfWeek - 1], color)
            days.add(newDay)
        }
    }

    // type 1 fert, 2 seed, 3 bug, 4 work, 5 power
    fun addDates(type: Int, fertDate: String, bugDate: String, seedDate: String, workDate: String, powerDate: String) {
        if(type == 0) {
            addLineDate(fertDate, 1, 1)
            addLineDate(seedDate, 2, 1)
            addLineDate(bugDate, 3, 1)
            addLineDate(workDate, 4, 1)
            addLineDate(powerDate, 5, 1)
        }
        else {
            when (type) {
                1 -> addLineDate(fertDate, type, 0)
                2 -> addLineDate(seedDate, type, 0)
                3 -> addLineDate(bugDate, type, 0)
                4 -> addLineDate(workDate, type, 0)
                5 -> addLineDate(powerDate, type, 0)
            }
        }
    }

    fun addLineDate(dateLst: String, type: Int, block: Int) {
        var color = 0;
        when (type) {
            1 -> color = COLOR21
            2 -> color = COLOR22
            3 -> color = COLOR23
            4 -> color = COLOR24
            5 -> color = COLOR25
        }
        var dataArr = dateLst.split(";")
        if(dataArr.size > 0) {
            for(i in 0..dataArr.size - 1) {
                groupID++
                val dateArr = dataArr[i].split("\t")
                if(dateArr.size == 4) {
                    val fromArr = dateArr[0].split(".")
                    val toArr = dateArr[1].split(".")

                    if(fromArr.size == 3 && toArr.size == 3) {
                        val date1 = Date(fromArr[0].toInt(), fromArr[1].toInt() - 1, fromArr[2].toInt())
                        val cal1 = Calendar.getInstance()
                        cal1.time = date1

                        val date2 = Date(toArr[0].toInt(), toArr[1].toInt() - 1, toArr[2].toInt())
                        val cal2 = Calendar.getInstance()
                        cal2.time = date2

                        var px = 0.0
                        var py = 0.0
                        var cnt = 1

                        while (cal1 <= cal2) {
                            val dt = "" + (cal1.get(Calendar.YEAR) - 1900) + "." + (cal1.get(Calendar.MONTH) + 1) + "." + cal1.get(Calendar.DATE)
                            for(j in 0..days.size - 1) {
                                if(days[j].info == dt && days[j].type == 0) {
                                    try {
                                        val newDay = Day(
                                            days[j].px, days[j].py + type * height,
                                            dateArr[3].toInt(), scale, type, dt, dateArr[2], groupID
                                        )
                                        var txt = ""
                                        //if (cnt == 0)
                                        //    txt = dateArr[2]
                                        //Log.i(TAG, "" + dateArr[3])
                                        newDay.setDay(
                                            days[j].px, days[j].py + type * height * block,
                                            days[j].width, height,
                                            dateArr[3].toInt(), txt, color
                                        )
                                        days.add(newDay)
                                        if (px == 0.0) px = days[j].px
                                        if (py == 0.0) py = days[j].py
                                        cnt++
                                    }
                                    catch (ex: Exception) {
                                        Log.e(TAG, "" + dataArr[i])
                                    }
                                }
                            }
                            cal1.add(Calendar.DAY_OF_MONTH, 1)
                        }

                        // add block border
                        if(cnt > 0) {
                            Log.i(TAG, "" + dateArr[2])
                        }
                    }
                }
            }
        }
    }

    fun addBlockDays(month: Int, xOff: Double, yOff: Double, type: Int) {
        val cCode = Locale.getDefault().country
        //Log.i(TAG, "country: " + cCode)
        var color = 0;
        when (type) {
            1 -> color = COLOR11
            2 -> color = COLOR12
            3 -> color = COLOR13
            4 -> color = COLOR14
            5 -> color = COLOR15
        }
        var px = 40.0 + xOff
        var py = 40.0 + yOff
        val dt = Date()
        val maxDays = arrayOf(31, if(dt.year % 4 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val newDay = Day(px, py, 0, scale, 0, monthNames[month - 1], "", 0)
        newDay.setDay(px * scale + width, py * scale - height, width  * 7, height, 0, "" + month + ". " + monthNames[month - 1], 0xFFFFFFFF.toInt())
        days.add(newDay)

        for(i in 1..7) {
            val newDay = Day(px + i * width, py, 0, scale, 0, dayNames2[i - 1], "", 0)
            newDay.setDay(px * scale + i * width,py * scale, width, height * 7, i, dayNames2[i - 1], 0xFFFFFFFF.toInt())
            days.add(newDay)
        }

        var add = 0.0
        for(i in 1..maxDays[month - 1]) {
            val date = Date(dt.year, month - 1, i)
            val cal = Calendar.getInstance()
            cal.time = date
            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            var weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)
            dayOfWeek -= 1
            if (dayOfWeek < 1) {
                dayOfWeek = 7
                if(cCode != "HU")
                    weekOfMonth -= 1
            }
            val info = "" + (dt.year + 1900) + "." + month + "." + i
            val toDay = "" + (dt.year + 1900) + "." + (dt.month + 1) + "." + dt.date
            Log.i(TAG, "" + info + " " + toDay)
            if(weekOfMonth == 0)
                add = height
            val newDay = Day(px + width * dayOfWeek, py + height * weekOfMonth, 0, scale, 0, info, "", 0)
            if(info == toDay)
                newDay.setDay(px * scale + width * dayOfWeek,py * scale + height * weekOfMonth + add, width, height, i, i.toString(), 0xFF216996.toInt())
            else
                newDay.setDay(px * scale + width * dayOfWeek,py * scale + height * weekOfMonth + add, width, height, i, i.toString(), color)
            days.add(newDay)
        }
    }

    fun selectGroup(grid: Int, sel: Boolean) {
        for (i in days.size - 1 downTo 0) {
            val item = days.get(i)
            if (item.groupid == grid) {
                if(sel) item.mode = Day.SELECTED
                else item.mode = Day.NORMAL
            }
        }
    }
}