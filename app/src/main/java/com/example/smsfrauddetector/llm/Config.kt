package com.example.smsfrauddetector.llm

enum class ConfigEditorType {
    NUMBER_SLIDER,
    BOOLEAN_SWITCH,
    DROPDOWN
}

enum class ValueType {
    INT, FLOAT, BOOLEAN, STRING
}

enum class ConfigKey(val label: String) {
    MAX_TOKENS("Max tokens"),
    TOPK("TopK"),
    TOPP("TopP"),
    TEMPERATURE("Temperature"),
    ACCELERATOR("Accelerator")
}

open class Config(
    val type: ConfigEditorType,
    open val key: ConfigKey,
    open val defaultValue: Any,
    open val valueType: ValueType,
    open val needReinitialization: Boolean = true
)

class NumberSliderConfig(
    override val key: ConfigKey,
    val sliderMin: Float,
    val sliderMax: Float,
    override val defaultValue: Any,
    override val valueType: ValueType,
    override val needReinitialization: Boolean = true
) : Config(ConfigEditorType.NUMBER_SLIDER, key, defaultValue, valueType, needReinitialization)
class DropDownConfig(
    override val key: ConfigKey,
    override val defaultValue: String,
    val options: List<String>,
    override val needReinitialization: Boolean = true
) : Config(ConfigEditorType.DROPDOWN, key, defaultValue, ValueType.STRING, needReinitialization)