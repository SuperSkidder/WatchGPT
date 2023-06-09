package me.superskidder.watchgpt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import me.superskidder.watchgpt.config.SimpleConfig

class SettingsFragment : Fragment() {

    private lateinit var apikey: EditText
    private lateinit var systemPrompt: EditText
    private lateinit var gptType: RadioGroup
    private lateinit var savebtn: Button
    private lateinit var speakout: Switch


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        apikey = view.findViewById(R.id.api_key)
        systemPrompt = view.findViewById(R.id.api_key2)
        gptType = view.findViewById(R.id.radioGroup)
        savebtn = view.findViewById(R.id.savebtn)
        speakout = view.findViewById(R.id.speakout)

        // 获取之前保存的设置
        val configManager = SimpleConfig(requireContext())
        apikey.setText(configManager.getString("apikey", ""))
        systemPrompt.setText(
            configManager.getString(
                "systemPrompt",
                "You are a helpful assistant."
            )
        )
        speakout.isChecked = configManager.getBoolean("speakout", false)
        val gptTypeValue = configManager.getString("gptType", "gpt-3.5-turbo")
        val radioButton = gptType.findViewById<RadioButton>(getRadioButtonId(gptTypeValue))
        radioButton?.isChecked = true
        apikey.setOnEditorActionListener { _, _, _ ->
            configManager.setString("apikey", apikey.text.toString())
            false
        }

        systemPrompt.setOnEditorActionListener { _, _, _ ->
            configManager.setString("systemPrompt", systemPrompt.text.toString())
            false
        }

        gptType.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = gptType.findViewById<RadioButton>(checkedId)
            configManager.setString("gptType", radioButton?.text.toString())
        }
        savebtn.setOnClickListener {
            saveConfig()
        }

        speakout.setOnClickListener {
            configManager.setBoolean("speakout", speakout.isChecked)
        }

        return view
    }

    fun saveConfig() {
        val configManager = SimpleConfig(requireContext())
        configManager.setString("apikey", apikey.text.toString())
        configManager.setString("systemPrompt", systemPrompt.text.toString())
    }

    private fun getRadioButtonId(value: String?): Int {
        return when (value) {
            "gpt-3.5-turbo" -> R.id.radioButton
            "gpt-4" -> R.id.radioButton2
            "gpt-3.5-turbo-0613" -> R.id.radioButton3
            else -> -1
        }
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
