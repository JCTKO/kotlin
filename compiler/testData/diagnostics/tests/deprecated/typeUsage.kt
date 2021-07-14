// FIR_IDENTICAL
@Deprecated("Class")
open class Obsolete {
    fun use() {}
}

@Deprecated("Class")
open class Obsolete2 @Deprecated("Constructor") constructor() {
    fun use() {}
}

class Properties {
    var n : <!DEPRECATION!>Obsolete<!>
        get() = <!DEPRECATION!>Obsolete<!>()
        set(value) {}
}