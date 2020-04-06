package com.healthmetrix.labres

import javax.inject.Inject

interface EonRepository {
    fun save(eon: Eon): Boolean
}

class InMemoryEonRepository @Inject constructor() : EonRepository {

    private val map: HashSet<Eon> = hashSetOf()

    override fun save(eon: Eon): Boolean {
        map.add(eon)
        return true
    }
}
