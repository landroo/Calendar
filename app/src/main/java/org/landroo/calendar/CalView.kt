package org.landroo.calendar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import org.landroo.calendar.ScaleView
import org.landroo.calendar.TouchInterface
import org.landroo.calendar.TouchUI
import org.landroo.calendar.Utils
import java.util.*
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class CalView : AppCompatActivity(), TouchInterface {


    private val SCROLL_INTERVAL = 10L
    private val SCROLL_ALPHA = 500
    private val SCROLL_SIZE = 16f
    private val GAP = 10
    private val TAG: String = "MainActivity"
    private val DESKTOPX: Int = 2200
    private val DESKTOPY: Int = 3500

    // virtual desktop
    private lateinit var nodeView: NodeView
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var deskWidth: Int = 0
    private var deskHeight: Int = 0

    // scroll plane
    private lateinit var scaleView: ScaleView
    private var sX = 0.0
    private var sY = 0.0
    private var mX = 0.0
    private var mY = 0.0
    private var zoomX = 1.0
    private var zoomY = 1.0
    private var xPos: Double = 0.0
    private var yPos: Double = 0.0
    private var afterMove = false
    private var paused = false

    // user event handler
    private lateinit var ui: TouchUI
    private var scrollTimer: Timer = Timer()
    private val scrollPaint1 = Paint()
    private val scrollPaint2 = Paint()
    private var scrollAlpha = SCROLL_ALPHA
    private var scrollBar = 0
    private var barPosX = 0.0
    private var barPosY = 0.0
    private val barPaint = Paint()

    // background
    private val tileSize = 80
    private lateinit var backBitmap: Bitmap
    private var backDrawable: Drawable? = null
    private var staticBack = false // fix or scrollable background
    private val backColor = Color.LTGRAY// background color
    private val rotation = 0.0
    private val rx = 0.0
    private val ry = 0.0
    private var longPress = 0
    private var back = ""

    private var scale: Float = 1f
    private val infoPaint = Paint()
    private val strokePaint = Paint()
    private val txtPaint = Paint()
    private var infoX = 0f
    private var infoY = 0f
    private var infoW = 0f
    private var infoH = 0f
    private var txtHeight = 18 * scale + 0.5f

    private lateinit var calClass :CalClass

    var fertDate = ""
    var bugDate = ""
    var seedDate = ""
    var workDate = ""
    var powerDate = ""
    var type = 0
    var selDay: Day? = null

    private inner class NodeView(context: Context) : ViewGroup(context) {

        // draw items
        override fun dispatchDraw(canvas: Canvas) {
            drawBack(canvas)
            drawItems(canvas)
            drawScrollBars(canvas)
            drawInfo(canvas)

            super.dispatchDraw(canvas)
        }

        override fun onLayout(b: Boolean, i: Int, i1: Int, i2: Int, i3: Int) {
            // main
            val child = this.getChildAt(0)
            child.layout(0, 0, displayWidth, displayHeight);
            //child?.layout(bottomViewRect.left, bottomViewRect.top, bottomViewRect.right, bottomViewRect.bottom)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(displayWidth, displayHeight)
            // main
            val child = this.getChildAt(0)
            if (child != null)
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        val b = intent.extras
        if (b != null) {
            fertDate = b.getString("fertDate").toString()
            bugDate = b.getString("bugDate").toString()
            seedDate = b.getString("seedDate").toString()
            workDate = b.getString("workDate").toString()
            powerDate = b.getString("powerDate").toString()
            type = b.getInt("type")
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        displayWidth = size.x
        displayHeight = size.y

        if (back != "") {
            val resId = resources.getIdentifier(back, "drawable", packageName)
            backBitmap = BitmapFactory.decodeResource(resources, resId)
            backDrawable = BitmapDrawable(backBitmap)
            backDrawable?.setBounds(0, 0, backBitmap.width, backBitmap.height)
        }

        nodeView = NodeView(this)
        setContentView(nodeView)

        ui = TouchUI(this)

        val mainView = layoutInflater.inflate(R.layout.cal_view, null) as RelativeLayout
        nodeView.addView(mainView)

        deskWidth = (DESKTOPX * resources.displayMetrics.density).toInt()
        deskHeight = (DESKTOPY * resources.displayMetrics.density).toInt()
        scaleView = ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, nodeView)

        if(type > 0) {
            resizeDesk((1600 * resources.displayMetrics.density).toInt(), (1600 * resources.displayMetrics.density).toInt())
        }

        calClass = CalClass(this)
        calClass.addYear(type)
        calClass.addDates(type, fertDate, bugDate, seedDate, workDate, powerDate)

        scale = resources.displayMetrics.density
        txtHeight = 18 * scale + 0.5f

        scrollPaint1.color = Color.GRAY
        scrollPaint1.isAntiAlias = true
        scrollPaint1.isDither = true
        scrollPaint1.style = Paint.Style.STROKE
        scrollPaint1.strokeJoin = Paint.Join.ROUND
        scrollPaint1.strokeCap = Paint.Cap.ROUND
        scrollPaint1.strokeWidth = SCROLL_SIZE

        scrollPaint2.color = 0xFF4AE2E7.toInt()
        scrollPaint2.isAntiAlias = true
        scrollPaint2.isDither = true
        scrollPaint2.style = Paint.Style.STROKE
        scrollPaint2.strokeJoin = Paint.Join.ROUND
        scrollPaint2.strokeCap = Paint.Cap.ROUND
        scrollPaint2.strokeWidth = SCROLL_SIZE

        strokePaint.color = 0xFF000000.toInt()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 5f

        infoPaint.color = 0xFFFFFFFF.toInt()
        infoPaint.style = Paint.Style.FILL

        txtPaint.textSize = txtHeight
    }

    public override fun onPause() {
        paused = true

        scrollTimer?.cancel()
        scrollTimer?.purge()

        scaleView?.stopTimer()

        //Log.i(TAG, "paused");
        super.onPause()
    }

    override fun onResume() {
        paused = false

        scrollTimer = Timer()
        scrollTimer.scheduleAtFixedRate(ScrollTask(), 0, SCROLL_INTERVAL)

        scaleView?.startTimer()

        //Log.i(TAG, "resumed");
        super.onResume()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return ui.tapEvent(event)
    }

    override fun onDown(x: Double, y: Double) {
        //Log.i(TAG, "onDown")
        afterMove = false
        scrollAlpha = SCROLL_ALPHA

        scaleView.onDown(x, y)

        sX = x / zoomX
        sY = y / zoomY

        mX = x / zoomX
        mY = y / zoomY

        xPos = scaleView.xPos
        yPos = scaleView.yPos

        scrollBar = checkBars(x, y)
        if (scrollBar === 1) {
            barPosX = x - barPosX
        } else if (scrollBar === 2) {
            barPosY = y - barPosY
        }
        else {

        }
    }

    override fun onUp(x: Double, y: Double) {
        //Log.i(TAG, "onUp")
        scaleView.onUp(x, y)

        scrollBar = 0

        longPress = 0
    }

    override fun onTap(x: Double, y: Double) {
        //Log.i(TAG, "onTap")
        scrollAlpha = SCROLL_ALPHA
        longPress = 60
        scrollBar = 0

        if(selDay != null) {
            calClass.selectGroup(selDay!!.groupid, false)
            selDay = null
        }

        val day = calClass.selectDate(x - xPos, y - yPos, zoomX, zoomY)
        if(day != null) {
            selDay = day
            //Log.i(TAG, "" + selDay!!.info + " " + selDay!!.id)
            calClass.selectGroup(selDay!!.groupid, true)
            showInfo()
        }

        xPos = scaleView.xPos
        yPos = scaleView.yPos

    }

    override fun onHold(x: Double, y: Double) {
    }

    override fun onMove(x: Double, y: Double) {
        //Log.i(TAG, "onMove")
        scrollAlpha = SCROLL_ALPHA

        mX = x / zoomX
        mY = y / zoomY

        val dx = mX - sX
        val dy = mY - sY

        if (scrollBar !== 0) {
            // vertical scroll
            if (scrollBar === 1) {
                val xp = -(x - barPosX) / (displayWidth / (deskWidth * zoomX))
                //Log.i(TAG, "" + xp)
                if (xp < 0 && xp > displayWidth - deskWidth * zoomX) {
                    xPos = xp
                }
            } else {
                val yp = -(y - barPosY) / (displayHeight / (deskHeight * zoomY))
                //Log.i(TAG, "" + yp)
                if (yp < 0 && yp > displayHeight - deskHeight * zoomY) {
                    yPos = yp
                }
            }
            scaleView.setPos(xPos, yPos)
            nodeView.postInvalidate()
        }
        else if (inInfo()) {
            infoX += dx.toFloat() * zoomX.toFloat()
            infoY += dy.toFloat() * zoomY.toFloat()

            nodeView.postInvalidate()
        }
        else {
            scaleView.onMove(x, y)
        }

        sX = mX
        sY = mY
    }

    override fun onDoubleTap(x: Double, y: Double) {
/*
        when(back) {
            "" -> back = "grid"
            "grid" -> back = "grid1"
            "grid1" -> back = "grid2"
            "grid2" -> back = ""
        }

        if (!back.equals("")) {
            val resId = resources.getIdentifier(back, "drawable", packageName)
            backBitmap = BitmapFactory.decodeResource(resources, resId)
            backDrawable = BitmapDrawable(backBitmap)
            backDrawable?.setBounds(0, 0, backBitmap.width, backBitmap.height)
            staticBack = false
        }
        else {
            staticBack = true
            backDrawable = null
        }

        nodeView.postInvalidate()
*/
    }

    override fun onSwipe(direction: Int, velocity: Double, x1: Double, y1: Double, x2: Double, y2: Double) {
        longPress = 0

        if (!afterMove)
            scaleView.onSwipe(direction, velocity, x1, y1, x2, y2)
    }

    override fun onZoom(mode: Int, x: Double, y: Double, distance: Double, xDiff: Double, yDiff: Double) {
        longPress = 0

        scaleView.onZoom(mode, x, y, distance, xDiff, yDiff)

        zoomX = scaleView.zoomX
        zoomY = scaleView.zoomY
    }

    override fun onRotate(mode: Int, x: Double, y: Double, angle: Double) {
    }

    override fun onFingerChange() {
    }

    private fun drawBack(canvas: Canvas) {
        if (backDrawable != null) {
            // static back or tiles
            if (staticBack) {
                if (backDrawable != null) {
                    backDrawable?.setBounds(0, 0, displayWidth, displayHeight)
                    backDrawable?.draw(canvas)
                } else {
                    canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                }
            }
            else {
                if (scaleView != null) {
                    xPos = scaleView.xPos
                    yPos = scaleView.yPos
                }
                var x = 0.0
                while (x < deskWidth) {
                    var y = 0.0
                    while (y < deskHeight) {
                        // distance of the tile center from the rotation center
                        val dis = Utils().getDist(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY)
                        // angle of the tile center from the rotation center
                        val ang = Utils().getAng(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY)

                        // coordinates of the block after rotation
                        val cx = dis * Math.cos((rotation + ang) * Utils().DEGTORAD) + rx * zoomX + xPos
                        val cy = dis * Math.sin((rotation + ang) * Utils().DEGTORAD) + ry * zoomY + yPos

                        if (cx >= -tileSize && cx <= displayWidth + tileSize && cy >= -tileSize && cy <= displayHeight + tileSize) {
                            backDrawable?.setBounds(0, 0, (tileSize * zoomX).toInt() + 1, (tileSize * zoomY).toInt() + 1)

                            canvas.save()
                            canvas.rotate(rotation.toFloat(), (rx * zoomX + xPos).toFloat(), (ry * zoomY + yPos).toFloat())
                            canvas.translate((x * zoomX + xPos).toFloat(), (y * zoomY + yPos).toFloat())
                            backDrawable?.draw(canvas)
                            canvas.restore()
                        }
                        y += tileSize
                    }
                    x += tileSize
                }
            }
        }
        else {
            when(type){
                0 -> canvas.drawColor(backColor)
                1 -> canvas.drawColor(calClass.COLOR11)
                2 -> canvas.drawColor(calClass.COLOR12)
                3 -> canvas.drawColor(calClass.COLOR13)
                4 -> canvas.drawColor(calClass.COLOR14)
                5 -> canvas.drawColor(calClass.COLOR15)
            }
        }
    }

    private fun drawItems(canvas: Canvas) {
        if (scaleView != null) {
            xPos = scaleView.xPos
            yPos = scaleView.yPos

            calClass.drawDays(canvas, xPos, yPos, zoomX, zoomY, displayWidth, displayHeight)
        }

    }

    private fun drawScrollBars(canvas: Canvas) {
        val xSize = displayWidth / (deskWidth * zoomX / displayWidth)
        val ySize = displayHeight / (deskHeight * zoomY / displayHeight)
        var x = displayWidth / (deskWidth * zoomX) * -xPos
        var y = displayHeight - SCROLL_SIZE - 2.0
        if (xSize < displayWidth) {
            if (scrollBar === 1) {
                canvas.drawLine(x.toFloat(), y.toFloat(), (x + xSize).toFloat(), y.toFloat(), scrollPaint2)
            } else {
                canvas.drawLine(x.toFloat(), y.toFloat(), (x + xSize).toFloat(), y.toFloat(), scrollPaint1)
            }
        }

        x = displayWidth - SCROLL_SIZE - 2.0
        y = displayHeight / (deskHeight * zoomY) * -yPos
        if (ySize < displayHeight) {
            if (scrollBar === 2) {
                canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + ySize).toFloat(), scrollPaint2)
            } else {
                canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + ySize).toFloat(), scrollPaint1)
            }
        }
    }

    private fun drawInfo(canvas: Canvas) {
        if(selDay != null) {
            val rect = RectF(infoX, infoY, infoX + infoW, infoY + infoH)
            canvas.drawRoundRect(rect, 20f, 20f, infoPaint)
            canvas.drawRoundRect(rect, 20f, 20f, strokePaint)

            val infoArr = selDay!!.descript.split("\n")
            for(i in 0.. infoArr.size - 1) {
                canvas.drawText(infoArr[i].toString(), infoX + 10 * scale, infoY + 30 * scale + i * txtHeight, txtPaint)
            }

        }
    }

    internal inner class ScrollTask : TimerTask() {
        override fun run() {
            if (paused)
                return

            if (longPress < 50) {
                longPress++
            }

            if (scrollAlpha > 32) {
                scrollAlpha--
                if (scrollAlpha > 255)
                    scrollPaint1.alpha = 255
                else
                    scrollPaint1.alpha = scrollAlpha
                nodeView.postInvalidate()
            }
        }
    }

    private fun checkBars(x: Double, y: Double): Int {
        val xSize = displayWidth / (deskWidth * zoomX / displayWidth)
        val ySize = displayHeight / (deskHeight * zoomY / displayHeight)
        var px = displayWidth / (deskWidth * zoomX) * -xPos
        var py = displayHeight - SCROLL_SIZE - 2.0
        //Log.i(TAG, "" + x + " " + xp + " " + (x+ xSize) + " " + y + " " + yp + " " + (y + SCROLL_SIZE));
        if (x > px && y > py - GAP && x < px + xSize && y < py + SCROLL_SIZE + GAP && xSize < displayWidth) {
            barPosX = px
            return 1
        }

        px = displayWidth - SCROLL_SIZE - 2.0
        py = displayHeight / (deskHeight * zoomY) * -yPos
        if (x > px - GAP && y > py && x < px + SCROLL_SIZE + GAP && y < py + ySize && ySize < displayHeight) {
            barPosY = py
            return 2
        }

        return 0
    }

    private fun resizeDesk(newW: Int, newH: Int) {
        deskWidth = newW
        deskHeight = newH
        scaleView.setSize(displayWidth, displayHeight, deskWidth, deskHeight)
        scaleView.setPos(xPos, yPos)
        scaleView.setZoom(zoomX, zoomY)
        nodeView.postInvalidate()
    }

    private fun showInfo() {
        infoW = 320 * scale
        infoH = txtHeight
        val infoArr = selDay!!.descript.split("\n")
        for(i in 0.. infoArr.size - 1) {
            var wArr = FloatArray(infoArr[i].toString().length)
            txtPaint.getTextWidths(infoArr[i].toString(), wArr)
            var wd = 0f
            for(j in 0..wArr.size - 1) {
                wd += wArr[j] / scale + 0.5f
            }
            if(wd * scale > infoW) infoW = wd * scale
            infoH += txtHeight
        }

        infoX = (displayWidth - infoW) / 2f
        infoY = (displayHeight - infoH) / 2f
    }

    private fun inInfo(): Boolean {
        var ret = false
        if(selDay != null) {
            if (mX * zoomX > infoX && mX * zoomX < infoX + infoW &&
                mY * zoomY > infoY && mY * zoomY < infoY + infoH
            ) ret = true
        }

        return ret
    }
}