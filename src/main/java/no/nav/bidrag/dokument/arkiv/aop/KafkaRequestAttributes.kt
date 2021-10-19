package no.nav.bidrag.dokument.arkiv.aop

import org.springframework.web.context.request.RequestAttributes


class KafkaRequestScopeAttributes : RequestAttributes {
    private val requestAttributeMap: MutableMap<String, Any> = HashMap()
    override fun getAttribute(name: String, scope: Int): Any? {
        return if (scope == RequestAttributes.SCOPE_REQUEST) {
            requestAttributeMap[name]
        } else null
    }

    override fun setAttribute(name: String, value: Any, scope: Int) {
        if (scope == RequestAttributes.SCOPE_REQUEST) {
            requestAttributeMap[name] = value
        }
    }

    override fun removeAttribute(name: String, scope: Int) {
        if (scope == RequestAttributes.SCOPE_REQUEST) {
            requestAttributeMap.remove(name)
        }
    }

    override fun getAttributeNames(scope: Int): Array<String?> {
        return if (scope == RequestAttributes.SCOPE_REQUEST) {
            requestAttributeMap.keys.toTypedArray()
        } else arrayOfNulls(0)
    }

    override fun registerDestructionCallback(name: String, callback: Runnable, scope: Int) {
        // Not Supported
    }

    override fun resolveReference(key: String): Any? {
        // Not supported
        return null
    }

    override fun getSessionId(): String? {
        return null
    }

    override fun getSessionMutex(): Any? {
        return null
    }
}