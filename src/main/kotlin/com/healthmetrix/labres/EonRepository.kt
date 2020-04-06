package com.healthmetrix.labres

interface EonRepository {
    fun save(eon: Eon): Boolean
}

class InMemoryEonRepository : EonRepository {

    private val map: HashSet<Eon> = hashSetOf()

    override fun save(eon: Eon): Boolean {
        map.add(eon)
        return true
    }

}