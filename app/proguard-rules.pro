# ProGuard rules for Lumi
# Retrofit/OkHttp 已移除，模型层走 Room 编译期生成，无需额外 keep 规则
-keepattributes Signature
-keepattributes *Annotation*
