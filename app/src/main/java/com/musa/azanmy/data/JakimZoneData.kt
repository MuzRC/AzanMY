package com.musa.azanmy.data

object JakimZoneData {

    val allZones: List<JakimZone> = listOf(
        // JOHOR
        JakimZone("JHR01", "Johor", "Pulau Aur dan Pulau Pemanggil", 2.4450, 104.5200),
        JakimZone("JHR02", "Johor", "Johor Bahru, Kota Tinggi, Mersing, Kulai", 1.4927, 103.7414),
        JakimZone("JHR03", "Johor", "Kluang, Pontian", 2.0301, 103.3185),
        JakimZone("JHR04", "Johor", "Batu Pahat, Muar, Segamat, Gemas Johor, Tangkak", 2.0467, 102.5689),

        // KEDAH
        JakimZone("KDH01", "Kedah", "Kota Setar, Kubang Pasu, Pokok Sena (Daerah Kecil)", 6.1248, 100.3678),
        JakimZone("KDH02", "Kedah", "Kuala Muda, Yan, Pendang", 5.6470, 100.4877),
        JakimZone("KDH03", "Kedah", "Padang Terap, Sik", 6.2564, 100.6844),
        JakimZone("KDH04", "Kedah", "Baling", 5.6761, 100.9178),
        JakimZone("KDH05", "Kedah", "Bandar Baharu, Kulim", 5.3717, 100.5553),
        JakimZone("KDH06", "Kedah", "Langkawi", 6.3500, 99.8000),
        JakimZone("KDH07", "Kedah", "Puncak Gunung Jerai", 5.7875, 100.4342),

        // KELANTAN
        JakimZone("KTN01", "Kelantan", "Bachok, Kota Bharu, Machang, Pasir Mas, Pasir Puteh, Tanah Merah, Tumpat, Kuala Krai, Mukim Chiku", 6.1254, 102.2381),
        JakimZone("KTN02", "Kelantan", "Gua Musang (Daerah Galas dan Bertam), Jeli, Jajahan Kecil Lojing", 4.8842, 101.9644),

        // MELAKA
        JakimZone("MLK01", "Melaka", "Seluruh Negeri Melaka", 2.1896, 102.2501),

        // NEGERI SEMBILAN
        JakimZone("NGS01", "Negeri Sembilan", "Tampin, Jempol", 2.4701, 102.2302),
        JakimZone("NGS02", "Negeri Sembilan", "Jelebu, Kuala Pilah, Rembau", 2.7389, 102.2487),
        JakimZone("NGS03", "Negeri Sembilan", "Port Dickson, Seremban", 2.7258, 101.9424),

        // PAHANG
        JakimZone("PHG01", "Pahang", "Pulau Tioman", 2.7902, 104.1698),
        JakimZone("PHG02", "Pahang", "Kuantan, Pekan, Rompin, Muadzam Shah", 3.8077, 103.3260),
        JakimZone("PHG03", "Pahang", "Jerantut, Temerloh, Maran, Bera, Chenor, Jengka", 3.4506, 102.4176),
        JakimZone("PHG04", "Pahang", "Bentong, Lipis, Raub", 3.5223, 101.9086),
        JakimZone("PHG05", "Pahang", "Genting Sempah, Janda Baik, Bukit Tinggi", 3.3500, 101.8500),
        JakimZone("PHG06", "Pahang", "Cameron Highlands, Genting Highlands, Bukit Fraser", 4.4721, 101.3801),

        // PERLIS
        JakimZone("PLS01", "Perlis", "Kangar, Padang Besar, Arau", 6.4414, 100.1986),

        // PULAU PINANG
        JakimZone("PNG01", "Pulau Pinang", "Seluruh Negeri Pulau Pinang", 5.4141, 100.3288),

        // PERAK
        JakimZone("PRK01", "Perak", "Tapah, Slim River, Tanjung Malim", 4.1977, 101.2630),
        JakimZone("PRK02", "Perak", "Kuala Kangsar, Sg. Siput, Ipoh, Batu Gajah, Kampar", 4.5975, 101.0901),
        JakimZone("PRK03", "Perak", "Lenggong, Pengkalan Hulu, Grik", 5.4167, 101.1333),
        JakimZone("PRK04", "Perak", "Temengor, Belum", 5.5500, 101.3500),
        JakimZone("PRK05", "Perak", "Kg Gajah, Teluk Intan, Bagan Datuk, Seri Iskandar, Beruas, Parit, Lumut, Sitiawan, Pulau Pangkor", 4.0259, 101.0213),
        JakimZone("PRK06", "Perak", "Selama, Taiping, Bagan Serai, Parit Buntar", 4.8519, 100.7416),
        JakimZone("PRK07", "Perak", "Bukit Larut", 4.8633, 100.7931),

        // SABAH
        JakimZone("SBH01", "Sabah", "Bahagian Sandakan (Timur), Bukit Garam, Semawang, Temanggong, Tambisan, Bandar Sandakan, Sukau", 5.8394, 118.1172),
        JakimZone("SBH02", "Sabah", "Beluran, Telupid, Pinangah, Terusan, Kuamut, Bahagian Sandakan (Barat)", 5.6433, 117.1311),
        JakimZone("SBH03", "Sabah", "Lahad Datu, Silabukan, Kunak, Sahabat, Semporna, Tungku, Bahagian Tawau (Timur)", 5.0268, 118.3270),
        JakimZone("SBH04", "Sabah", "Bandar Tawau, Balong, Merotai, Kalabakan, Bahagian Tawau (Barat)", 4.2447, 117.8912),
        JakimZone("SBH05", "Sabah", "Kudat, Kota Marudu, Pitas, Pulau Banggi, Bahagian Kudat", 6.8831, 116.8466),
        JakimZone("SBH06", "Sabah", "Gunung Kinabalu", 6.0753, 116.5588),
        JakimZone("SBH07", "Sabah", "Kota Kinabalu, Ranau, Kota Belud, Tuaran, Penampang, Papar, Putatan, Bahagian Pantai Barat", 5.9804, 116.0735),
        JakimZone("SBH08", "Sabah", "Pensiangan, Keningau, Tambunan, Nabawan, Bahagian Pendalaman (Atas)", 5.3378, 116.1602),
        JakimZone("SBH09", "Sabah", "Beaufort, Kuala Penyu, Sipitang, Tenom, Long Pasia, Membakut, Weston, Bahagian Pendalaman (Bawah)", 5.3473, 115.7455),

        // SARAWAK
        JakimZone("SWK01", "Sarawak", "Limbang, Lawas, Sundar, Trusan", 4.7500, 115.0000),
        JakimZone("SWK02", "Sarawak", "Miri, Niah, Bekenu, Sibuti, Marudi", 4.3995, 113.9914),
        JakimZone("SWK03", "Sarawak", "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu", 3.1667, 113.0333),
        JakimZone("SWK04", "Sarawak", "Sibu, Mukah, Dalat, Song, Igan, Oya, Balingian, Kanowit, Kapit", 2.2873, 111.8305),
        JakimZone("SWK05", "Sarawak", "Sarikei, Matu, Julau, Rajang, Daro, Bintangor, Belawai", 2.1167, 111.5167),
        JakimZone("SWK06", "Sarawak", "Lubok Antu, Sri Aman, Roban, Debak, Kabong, Lingga, Engkelili, Betong, Spaoh, Pusa, Saratok", 1.2375, 111.4619),
        JakimZone("SWK07", "Sarawak", "Serian, Simunjan, Samarahan, Sebuyau, Meludam", 1.1750, 110.5667),
        JakimZone("SWK08", "Sarawak", "Kuching, Bau, Lundu, Sematan", 1.5535, 110.3593),
        JakimZone("SWK09", "Sarawak", "Zon Khas (Kampung Patarikan)", 4.8667, 115.3833),

        // SELANGOR
        JakimZone("SGR01", "Selangor", "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Shah Alam", 3.0738, 101.5183),
        JakimZone("SGR02", "Selangor", "Kuala Selangor, Sabak Bernam", 3.3500, 101.2500),
        JakimZone("SGR03", "Selangor", "Klang, Kuala Langat", 3.0449, 101.4456),

        // TERENGGANU
        JakimZone("TRG01", "Terengganu", "Kuala Terengganu, Marang, Kuala Nerus", 5.3117, 103.1324),
        JakimZone("TRG02", "Terengganu", "Besut, Setiu", 5.8333, 102.5500),
        JakimZone("TRG03", "Terengganu", "Hulu Terengganu", 5.0667, 103.0167),
        JakimZone("TRG04", "Terengganu", "Dungun, Kemaman", 4.7667, 103.4167),

        // WILAYAH PERSEKUTUAN
        JakimZone("WLY01", "Wilayah Persekutuan", "Kuala Lumpur, Putrajaya", 3.1390, 101.6869),
        JakimZone("WLY02", "Wilayah Persekutuan", "Labuan", 5.2831, 115.2308)
    )

    fun findByCode(code: String): JakimZone {
        return allZones.find { it.code.equals(code.trim(), ignoreCase = true) }
            ?: allZones.first { it.code == "WLY01" }
    }

    fun search(query: String): List<JakimZone> {
        val q = query.trim()
        if (q.isEmpty()) return allZones
        return allZones.filter {
            it.code.contains(q, ignoreCase = true) ||
                it.state.contains(q, ignoreCase = true) ||
                it.areas.contains(q, ignoreCase = true)
        }
    }

    fun groupedByState(): Map<String, List<JakimZone>> {
        return allZones.groupBy { it.state }
    }
}
