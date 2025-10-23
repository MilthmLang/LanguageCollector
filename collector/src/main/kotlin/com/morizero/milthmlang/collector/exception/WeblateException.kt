package com.morizero.milthmlang.collector.exception

import java.io.IOException

class WeblateException(val statusCode: Int, val reasonPhrase: String) : IOException(reasonPhrase)