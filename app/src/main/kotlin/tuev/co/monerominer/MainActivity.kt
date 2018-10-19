package tuev.co.monerominer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import tuev.co.tumine.*
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.full.memberProperties

class MainActivity : AppCompatActivity() {

    private lateinit var infoPassing: InfoPassing
    private lateinit var preferences: SharedPreferences
    private var mineConnector: MineConnector? = null

    private val outputInfoTextViews = HashMap<String, TextView>()

    private var mOutputInfo = OutputInfo()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)


        infoPassing = InfoPassing(this@MainActivity)

        val cores = infoPassing.availableCores
        // write suggested cores usage into editText
        var suggested = if (preferences.contains("threads")) preferences.getInt("threads", 0) else cores / 2
        if (preferences.contains("username")) {
            username.setText(preferences.getString("username", ""))
        }
        if (preferences.contains("password")) {
            password.setText(preferences.getString("password", ""))
        }
        if (suggested <= 1) suggested = 1
        threads.setText(Integer.toString(suggested))
        allthreads.setText(Integer.toString(cores))
        username.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                preferences.edit().putString("username", p0?.toString() ?: "").apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                preferences.edit().putString("password", p0?.toString() ?: "").apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        threads.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if(p0?.isEmpty() == false) {
                    preferences.edit().putInt("threads", p0.toString().toInt()).apply()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        start.setOnClickListener { startMining() }
        stop.setOnClickListener { stopMining() }
        check.setOnClickListener {
            val url = "https://android-miner.tuev-co.eu"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        advhash.setOnClickListener {
            mineConnector?.requestHashratePerThread()
        }
        AlertDialog.Builder(this)
                .setTitle("Performance Warning!")
                .setMessage("All the colored information presented is only for aesthetic and demonstration purposes.\n" +
                        "It uses Spannable String and Reflection and is EXTREMELY inefficiently generated!")
                .setPositiveButton("OK") { _, _ -> }
                .show()
        updateOutputInfo(true)

        addMineConnector()

        //InfoPassing.startMiningServiceRestoreState(this)

    }

    private fun addMineConnector() {
        if (mineConnector == null) {
            mineConnector = MineConnector(object : OnMessageReceived {
                override fun connected() {
                    Toast.makeText(this@MainActivity, "Connected to Miner", Toast.LENGTH_LONG).show()
                }

                override fun messageReceived(message: OutputInfo) {
                    mOutputInfo = message
                    Log.wtf("recv", "messageReceived")
                    when (message.lastChangedValue) {
                        OutputHelperClasses.ChangedValue.hashrate -> {
                            speed.text = message.hashrate?.Highest?.toString() ?: "0"
                        }
                        OutputHelperClasses.ChangedValue.lastMiningJobResult -> {
                            accepted.text = message.lastMiningJobResult?.accepted?.toString() ?: "0"
                        }
                    }

                    for (prop in OutputInfo::class.memberProperties) {
                        if (prop.name == message.lastChangedValue?.name) {
                            val view = outputInfoTextViews[prop.name] ?: break
                            if (message.lastChangedValue == OutputHelperClasses.ChangedValue.lastMiningJobResult
                                    || message.lastChangedValue == OutputHelperClasses.ChangedValue.lastMiningJob
                                    || message.lastChangedValue == OutputHelperClasses.ChangedValue.lastError) {
                                scroll_view.post {
                                    scroll_view.smoothScrollTo(0, view.bottom)
                                }
                            }
                            view.setText(reflectionToString(prop.name, prop.get(mOutputInfo)), TextView.BufferType.SPANNABLE)
                            val builder = SpannableStringBuilder()
                            var start = builder.length
                            builder.append("LastChangedValue:\n")
                            builder.setSpan(ForegroundColorSpan(green), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            builder.setSpan(RelativeSizeSpan(1.4f), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            start = builder.length
                            builder.append("${prop.name}\n")
                            builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            outputInfoTextViews["lastChangedValue"]?.setText(builder, TextView.BufferType.SPANNABLE)
                            break
                        }
                    }
                }
            }, this, false)
        }
    }

    private fun updateOutputInfo(create: Boolean) {
        for (prop in OutputInfo::class.memberProperties) {
            /*val layoutInflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row: LinearLayout = layoutInflater.inflate(R.layout.output_row, null) as LinearLayout
            row.findViewById<TextView>(R.id.title).text = prop.name*/
            if (create) {
                val itemText = TextView(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    itemText.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
                } else {
                    itemText.setTextAppearance(this, R.style.TextAppearance_AppCompat_Caption)
                }
                itemText.setText(reflectionToString(prop.name, prop.get(mOutputInfo)),  TextView.BufferType.SPANNABLE)
                outputInfoTextViews[prop.name] = itemText
                output_info.addView(itemText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            } else {
                val text = outputInfoTextViews[prop.name]
                text?.setText(reflectionToString(prop.name, prop.get(mOutputInfo)),  TextView.BufferType.SPANNABLE)
            }
        }
    }

    private val green: Int = Color.parseColor("#4CAF50")
    private val blue: Int = Color.parseColor("#03A9F4")
    private val blgray: Int = Color.parseColor("#CFD8DC")

    fun reflectionToString(propName: String, obj: Any?): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (obj == null) {
            val start = builder.length
            builder.append("No data for $propName\n")
            builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(Color.LTGRAY), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }
        var start = builder.length
        builder.append("${propName.capitalize()}:\n")
        builder.setSpan(ForegroundColorSpan(green), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(1.4f), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (basicObjectToStirng(builder, obj, blgray)) {
            return builder
        }

        for (prop in obj.javaClass.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }) {
            prop.isAccessible = true
            val value = prop.get(obj)
            if (!value?.toString().isNullOrBlank()) {
                start = builder.length
                builder.append("${prop.name.capitalize()}: ")
                val basic = value is String || value is Boolean || value is Int || value is Float || value is Enum<*>
                if (basic) {
                    builder.setSpan(ForegroundColorSpan(blue), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    builder.setSpan(ForegroundColorSpan(green), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (value is List<*>) {
                    start = builder.length
                    builder.append("${value.joinToString(" | ")}\n\n")
                    builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (!basicObjectToStirng(builder, value, blgray)) {
                    val clazz: Class<*>? = value.javaClass
                    val fields = clazz?.declaredFields
                    if (fields != null) {
                        for (f in fields) {
                            if (!Modifier.isStatic(f.modifiers)) {
                                try {
                                    f.isAccessible = true
                                    start = builder.length
                                    builder.append("${f.name.capitalize()}: ")
                                    builder.setSpan(ForegroundColorSpan(blue), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                    start = builder.length
                                    builder.append("${f.get(value)}\n")
                                    builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                } catch (e: IllegalAccessException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    builder.append("\n")
                }
            }
        }
        //builder.append("\n")
        return builder
    }

    fun basicObjectToStirng(builder: SpannableStringBuilder, obj: Any, spanColor: Int): Boolean {
        var start: Int
        if (obj is String || obj is Boolean || obj is Int || obj is Float ) {
            start = builder.length
            builder.append("$obj\n")
            builder.setSpan(ForegroundColorSpan(spanColor), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }
        if (obj is Enum<*>) {
            start = builder.length
            builder.append("${obj.name}\n")
            builder.setSpan(ForegroundColorSpan(spanColor), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }
        if (obj is ArrayList<*>) {
            for (item in obj) {
                val clazz: Class<*>? = item.javaClass
                val fields = clazz?.declaredFields
                if (fields != null) {
                    for (f in fields) {
                        if (!Modifier.isStatic(f.modifiers)) {
                            try {
                                f.isAccessible = true
                                start = builder.length
                                builder.append("${f.name.capitalize()}: ")
                                builder.setSpan(ForegroundColorSpan(blue), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                start = builder.length
                                builder.append("${f.get(item)}\n")
                                builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            } catch (e: IllegalAccessException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                builder.append("\n")
            }
            return true
        }
        return false
    }

    private var running: Boolean = false

    private fun startMining() {
        if (running) {
            Toast.makeText(this, "It's already running, WHY YOU DO THIS?", Toast.LENGTH_LONG).show()
            return
        }
        running = true
        infoPassing.minerConfig.pools.add(Pool(pool.text.toString(), username.text.toString(), password.text.toString()))
        //infoPassing.pools.add(Pool("killallasics.moneroworld.com:3333", "9v4vTVwqZzfjCFyPi7b9Uv1hHntJxycC4XvRyEscqwtq8aycw5xGpTxFyasurgf2KRBfbdAJY4AVcemL1JCegXU4EZfMtaz", "oneplus"))
        infoPassing.minerConfig.coresToUse = Integer.parseInt(threads.text.toString())
        infoPassing.minerConfig.coresWhenInAJob = if (infoPassing.availableCores / 4 < 1) 1 else infoPassing.availableCores / 4
        infoPassing.minerConfig.updateOverInternet = false
        infoPassing.minerOutput.debugParams = true
        infoPassing.minerConfig.useSSL = true
        infoPassing.miningInAndroid.keepCPUawake = true
        infoPassing.miningInAndroid.checkIfRunningEvery5mins = true


        //infoPassing.questionableUsefulness.cpuPriority = 5
        //infoPassing.questionableUsefulness.cpuAffinity = optimalCPUaffinity()

        infoPassing.miningInAndroid.notificationLoaderClass = NotificationGetter::class.java
        infoPassing.minerOutput.isBasicLogging = true

        Log.d(javaClass.simpleName, "startMineService")
        infoPassing.saveState()
        Handler().postDelayed({infoPassing.startMiningService()}, 1 * 20 * 100)

    }

    private fun stopMining() {
        infoPassing.stopMiningService()
        running = false
        accepted.text = "0"
        speed.text = "0"
        mOutputInfo = OutputInfo()
        updateOutputInfo(false)
    }

    fun optimalCPUaffinity(): String {
        val cores = infoPassing.availableCores
        var optimalBinary: Long = 0
        if (cores == 8) {
            /**
             * this means - use core 5,6,7 and 8
             * 0 - don't use, 1 - use
             * read from right to left
             */
            optimalBinary = 11110000
        } else if (cores == 4) {
            /**
             * this means - use core 3,4
             * 0 - don't use, 1 - use
             * read from right to left
             */
            optimalBinary = 1100
        }
        return convertBinaryToDecimal(optimalBinary).toString()
    }

    private fun convertBinaryToDecimal(numIn: Long): Int {
        var num = numIn
        var decimalNumber = 0
        var i = 0
        var remainder: Long

        while (num.toInt() != 0) {
            remainder = num % 10
            num /= 10
            decimalNumber += (remainder * Math.pow(2.0, i.toDouble())).toInt()
            ++i
        }
        return decimalNumber
    }

    override fun onDestroy() {
        mineConnector?.detach()
        super.onDestroy()
    }
}
