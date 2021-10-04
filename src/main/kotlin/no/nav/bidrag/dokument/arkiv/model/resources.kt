package no.nav.bidrag.dokument.arkiv.model


class ResourceByDiscriminator<T>(private val resources: Map<Discriminator, T>) {

    init {
        if (resources.isEmpty()) {
            throw ResourceDiscriminatorException("Minst en ressurs må ligge i ressurs-mappen")
        }
    }

    fun get(discriminator: Discriminator) = resources[discriminator] ?: throw ResourceDiscriminatorException("Ingen ressurs for $discriminator")

    override fun toString() = "${resources.size} ${resources.firstNotNullOf { it.javaClass.simpleName }} med ${resources.keys}"
}

enum class Discriminator {
    REGULAR_USER, SERVICE_USER;
}
