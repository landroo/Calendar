package org.landroo.calendar

import android.content.res.AssetFileDescriptor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import org.landroo.calendar.Utils

class Day(px: Double, py: Double, id: Int, scale: Float, type: Int, info: String, descript: String, groupid: Int){

    companion object {
        const val RECTANGLE = 1

        const val NORMAL = 0
        const val SELECTED = 1
    }

    var corner = 12

    var px: Double = px// X position
    var py: Double = py// Y position

    var id: Int = id
    var groupid = groupid

    var mode: Int = 0
    var type: Int = type

    var angle = 0.0

    var width: Double = 320.0
    var height: Double = 240.0

    var path: Path = Path()
    var bound: Path = Path()
    var points: ArrayList<Utils.Point2D> = ArrayList()

    var rect: RectF = RectF()

    var text: String = ""
    var textWidth = 0

    var info = info
    var descript = descript

    var color: Int = 0xFFAAAAAA.toInt()

    private var scale = scale

    private var xarr: DoubleArray
    private var yarr: DoubleArray

    var u: Double = 0.0
    var v: Double = 0.0

    init {
        xarr = DoubleArray(points.size)
        yarr = DoubleArray(points.size)

        var paint: Paint = Paint()
        paint.textSize = 16f * scale

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        textWidth = bounds.width()
    }

    fun setDay(px: Double, py: Double, width: Double, height: Double, id: Int, text: String, color:Int) {
        this.px = px
        this.py = py
        this.width = width
        this.height = height
        this.id = id
        this.text = text
        this.color = color

        setRect()
        setPath()

        xarr = DoubleArray(points.size)
        yarr = DoubleArray(points.size)

    }

    private fun setPath() {
        var first = true
        for (pnt in points) {
            if (first) {
                path.moveTo(pnt.x.toFloat(), pnt.y.toFloat())
                first = false
            }
            path.lineTo(pnt.x.toFloat(), pnt.y.toFloat())
        }
    }

    fun isInside(posx: Double, posy: Double, zx: Double, zy: Double): Boolean {
        var i = 0
        var pf: Utils.Point2D
        for (pnt in points) {
            pf = Utils().rotatePnt((px + u) * zx, (py + v) * zy, (pnt.x + px) * zx, (pnt.y + py) * zy, angle * Utils().DEGTORAD)
            xarr[i] = pf.x
            yarr[i] = pf.y
            i++
        }

        return Utils().ponitInPoly(xarr, yarr, posx, posy)
    }

    fun setTxt(txt: String) {
        text = txt

        var paint: Paint = Paint()
        paint.textSize = 16f * scale

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        width = bounds.width().toDouble() + paint.textSize
        if(width < 320) width = 320.0
        height = width * (3.0 / 4.0)
        //setShapes(true)

        textWidth = bounds.width()
    }

    private fun setBound() {
        bound.moveTo(0f, 0f)
        bound.lineTo(width.toFloat(), 0f)
        bound.lineTo(width.toFloat(), height.toFloat())
        bound.lineTo(0f, height.toFloat())
        bound.lineTo(0f, 0f)

        rect.left = width.toFloat() - corner * scale
        rect.top = height.toFloat() - corner * scale
        rect.right = width.toFloat()
        rect.bottom = height.toFloat()
    }

    private fun setRect() {
        points.add(Utils.Point2D(0.0, 0.0))
        points.add(Utils.Point2D(width, 0.0))
        points.add(Utils.Point2D(width, height))
        points.add(Utils.Point2D(0.0, height))
        points.add(Utils.Point2D(0.0, 0.0))
    }
}