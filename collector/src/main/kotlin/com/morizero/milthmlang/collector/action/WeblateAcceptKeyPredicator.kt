package com.morizero.milthmlang.collector.action

import java.io.Serializable

interface WeblateAcceptKeyPredicator: Serializable {
    operator fun invoke(key: String): Boolean
}
