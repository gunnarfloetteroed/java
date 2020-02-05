/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package stockholm.ihop4.tollzonepassagedata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class TollZonePassageDataSpecification {

	static final Map<String, String> ingoingChargingPoint2link;

	static final Map<String, String> outgoingChargingPoint2link;

	static final Map<String, String> chargingPoint2link;

	static {
		ingoingChargingPoint2link = Collections.unmodifiableMap(ingoingChargingPoint2link());
		outgoingChargingPoint2link = Collections.unmodifiableMap(outgoingChargingPoint2link());
		chargingPoint2link = Collections.unmodifiableMap(TollZonePassageDataSpecification.chargingPoint2link());
	}

	private static Map<String, String> ingoingChargingPoint2link() {
		final Map<String, String> chargingPoint2link = new LinkedHashMap<>();

		// 22626
		// Danvikstull infart 1
		// 22626_AB --> 22626_W
		chargingPoint2link.put("11", "22626_W");

		// 110210
		// Danvikstull utfart 2
		// 110210_AB --> 110210_E
		// OUT chargingPoint2link.put("12", "110210_E");

		// 6114
		// Skansbron infart 3
		// Skansbron utfart 4
		// 6114_AB --> 6114_S
		// 6114_BA --> 6114_N
		// Skansbron infart 3 6114
		chargingPoint2link.put("21", "6114_N");
		// Skansbron utfart 4 6114
		// OUT chargingPoint2link.put("22", "6114_S");

		// 80449
		// Skanstullbron infart 5
		// 80449_AB --> 80449_N
		chargingPoint2link.put("31", "80449_N");

		// 28480
		// Skanstullbron utfart 6
		// 28480_AB --> 28480_S
		// OUT chargingPoint2link.put("32", "28480_S");

		// 9216
		// Johanneshovbron infart 7
		// 9216_AB --> 9216_N
		chargingPoint2link.put("41", "9216_N");

		// 18170
		// Johanneshovbron utfart 8
		// 18170_AB --> 18170_SE
		// OUT chargingPoint2link.put("42", "18170_SE");

		// 25292
		// Liljeholmsbron infart 9
		// 25292_AB --> 25292_NE
		chargingPoint2link.put("51", "25292_NE");

		// 2453
		// Liljeholmsbron utfart 10
		// 2453_AB --> 2453_SW
		// OUT chargingPoint2link.put("52", "2453_SW");

		// 74188
		// Stora Essingen infart 11
		// 74188_AB --> 74188_NW
		chargingPoint2link.put("61", "74188_NW");

		// 53064
		// Stora Essingen utfart 12
		// 53064_AB --> 53064_SE
		// OUT chargingPoint2link.put("62", "53064_SE");

		// 51930
		// Stora Essingen infart 13
		// 51930_AB --> 51930_W
		chargingPoint2link.put("63", "51930_W");

		// 44566
		// Lilla Essingen utfart 14
		// 44566_AB --> 44566_NE
		// OUT chargingPoint2link.put("72", "44566_NE");

		// 92866
		// Drottningholmsvägen infart från Bromma 15
		// 92866_AB --> 92866_E
		chargingPoint2link.put("81", "92866_E");

		// 77626
		// Drottningholmsvägen utfart mot Bromma 16
		// 77626_AB --> 77626_W
		// OUT chargingPoint2link.put("82", "77626_W");

		// 127169
		// Drottningholmsvägen infart från EL 17
		// [not encoded]

		// 124799
		// Drottningholmsvägen utfart mot EL 18
		// 124799_AB --> 124791_W
		// OUT chargingPoint2link.put("84", "124791_W");

		// 119586
		// Essingeleden södergående från Bromma 19
		// 119586_AB --> 119586_S
		chargingPoint2link.put("85", "119586_S");

		// 120732
		// Essingleden norrgående mot Bromma 20
		// 120732_AB --> 120732_W
		// OUT chargingPoint2link.put("86", "120732_W");

		// 74191
		// Essingeleden södergående 21
		// 74191_AB --> 74191_S
		chargingPoint2link.put("87", "74191_S");

		// 52300
		// Essingeleden norrgående 22
		// 52300_AB --> 52300_N
		// OUT chargingPoint2link.put("88", "52300_N");

		// 95249
		// Kristineberg Avfart Essingeleden S 23
		// 95249_AB --> REMOVED: 95249_NE
		// [removed in network cleaning]

		// 52301
		// Kristineberg Påfart Essingeleden N 24
		// 52301_AB --> 52301_N
		// OUT chargingPoint2link.put("94", "52301_N");

		// 128236
		// Kristineberg Avfart Essingeleden N 25
		// [not encoded]

		// 128234
		// Kristineberg Påfart Essingeleden S 26
		// [not encoded]

		// 122017
		// Klarastrandsleden infart 27
		// Klarastrandsleden utfart 28
		// 122017_AB --> 122017_NW
		// 122017_BA --> 122017_SE
		// Klarastrandsleden infart 27 122017
		chargingPoint2link.put("101", "122017_SE");
		// Klarastrandsleden utfart 28 122017
		// OUT chargingPoint2link.put("102", "122017_NW");

		// 121908
		// Ekelundsbron infart 29
		// Ekelundsbron utfart 30
		// 121908_AB --> 121908_N
		// 121908_BA --> 121908_S
		// Ekelundsbron infart 29 121908
		chargingPoint2link.put("111", "121908_S");
		// Ekelundsbron utfart 30 121908
		// OUT chargingPoint2link.put("112", "121908_N");

		// 52416
		// Tomtebodavägen infart NY 31
		// Tomtebodavägen utfart NY 32
		// 52416_AB --> REMOVED: 52416_SW
		// 52416_BA --> 52416_NE
		// [problematic, unclear which of the two was cleaned]

		// 108353
		// Solnavägen infart 33
		// 108353_AB --> 108353_SE
		chargingPoint2link.put("131", "108353_SE");

		// 113763
		// Solnavägen utfart 34
		// 113763_AB --> 113763_NW
		// OUT chargingPoint2link.put("132", "113763_NW");

		// 101350
		// Norrtull Sveavägen utfart 36
		// 101350_AB --> 101350_NW
		// OUT chargingPoint2link.put("146", "101350_NW");

		// 128229
		// Norrtull tillfällig väg 38
		// [not encoded]

		// 34743
		// Norra stationsgatan utfart 39
		// 34743_AB --> 34743_NE
		// OUT chargingPoint2link.put("148", "34743_NE");

		// 54215
		// Ekhagen Avfart E18S 40
		// 54215_AB --> 54215_S
		chargingPoint2link.put("211", "54215_S");

		// 116809
		// Ekhagen Påfart E18N 41
		// 116809_AB --> 116809_NW
		// OUT chargingPoint2link.put("212", "116809_NW");

		// 74955
		// Ekhagen Avfart E18N 42
		// 74955_AB --> 74955_N
		chargingPoint2link.put("213", "74955_N");

		// 35466
		// Ekhagen Påfart E18S 43
		// 35466_AB --> 35466_SE
		// OUT chargingPoint2link.put("214", "35466_SE");

		// 56348
		// Frescati infart 44
		// Frescati utfart 45
		// 56348_AB --> REMOVED: 56348_W
		// 56348_BA --> REMOVED: 56348_E
		// [removed when cleaning network]

		// 127555
		// Universitetet infart 46
		// [not encoded]

		// 42557
		// Universitetet utfart 47
		// 42557_AB --> 42557_N
		// OUT chargingPoint2link.put("232", "42557_N");

		// 129132
		// Roslagstull infart 48
		// 127146
		// Roslagstull utfart 49
		// [not encoded]

		// 128187
		// Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50
		// 128192
		// Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51
		// 128204
		// Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52
		// 128219
		// Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53
		// 128215
		// Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54
		// [not encoded]

		// 23370
		// Ropsten Infart till Norra Hamnvägen 55
		// 23370_AB --> 23370_NE
		chargingPoint2link.put("261", "23370_NE");

		// 43117
		// Ropsten Utfart från Norra Hamnvägen 56
		// 43117_AB --> 43117_N
		// OUT chargingPoint2link.put("262", "43117_N");

		// 125185
		// Ropsten Infart mot Hjorthagen 57
		// 125185_AB --> 125167_N
		chargingPoint2link.put("263", "125167_N");

		// 58297
		// Ropsten Utfart från Hjorthagen 58
		// 58297_AB --> 58297_SE
		// OUT chargingPoint2link.put("264", "58297_SE");

		return chargingPoint2link;
	}

	private static Map<String, String> outgoingChargingPoint2link() {
		final Map<String, String> chargingPoint2link = new LinkedHashMap<>();

		// 22626
		// Danvikstull infart 1
		// 22626_AB --> 22626_W
		// IN chargingPoint2link.put("11", "22626_W");

		// 110210
		// Danvikstull utfart 2
		// 110210_AB --> 110210_E
		chargingPoint2link.put("12", "110210_E");

		// 6114
		// Skansbron infart 3
		// Skansbron utfart 4
		// 6114_AB --> 6114_S
		// 6114_BA --> 6114_N
		// Skansbron infart 3 6114
		// IN chargingPoint2link.put("21", "6114_N");
		// Skansbron utfart 4 6114
		chargingPoint2link.put("22", "6114_S");

		// 80449
		// Skanstullbron infart 5
		// 80449_AB --> 80449_N
		// IN chargingPoint2link.put("31", "80449_N");

		// 28480
		// Skanstullbron utfart 6
		// 28480_AB --> 28480_S
		chargingPoint2link.put("32", "28480_S");

		// 9216
		// Johanneshovbron infart 7
		// 9216_AB --> 9216_N
		// IN chargingPoint2link.put("41", "9216_N");

		// 18170
		// Johanneshovbron utfart 8
		// 18170_AB --> 18170_SE
		chargingPoint2link.put("42", "18170_SE");

		// 25292
		// Liljeholmsbron infart 9
		// 25292_AB --> 25292_NE
		// IN chargingPoint2link.put("51", "25292_NE");

		// 2453
		// Liljeholmsbron utfart 10
		// 2453_AB --> 2453_SW
		chargingPoint2link.put("52", "2453_SW");

		// 74188
		// Stora Essingen infart 11
		// 74188_AB --> 74188_NW
		// IN chargingPoint2link.put("61", "74188_NW");

		// 53064
		// Stora Essingen utfart 12
		// 53064_AB --> 53064_SE
		chargingPoint2link.put("62", "53064_SE");

		// 51930
		// Stora Essingen infart 13
		// 51930_AB --> 51930_W
		// IN chargingPoint2link.put("63", "51930_W");

		// 44566
		// Lilla Essingen utfart 14
		// 44566_AB --> 44566_NE
		chargingPoint2link.put("72", "44566_NE");

		// 92866
		// Drottningholmsvägen infart från Bromma 15
		// 92866_AB --> 92866_E
		// IN chargingPoint2link.put("81", "92866_E");

		// 77626
		// Drottningholmsvägen utfart mot Bromma 16
		// 77626_AB --> 77626_W
		chargingPoint2link.put("82", "77626_W");

		// 127169
		// Drottningholmsvägen infart från EL 17
		// [not encoded]

		// 124799
		// Drottningholmsvägen utfart mot EL 18
		// 124799_AB --> 124791_W
		chargingPoint2link.put("84", "124791_W");

		// 119586
		// Essingeleden södergående från Bromma 19
		// 119586_AB --> 119586_S
		// IN chargingPoint2link.put("85", "119586_S");

		// 120732
		// Essingleden norrgående mot Bromma 20
		// 120732_AB --> 120732_W
		chargingPoint2link.put("86", "120732_W");

		// 74191
		// Essingeleden södergående 21
		// 74191_AB --> 74191_S
		// IN chargingPoint2link.put("87", "74191_S");

		// 52300
		// Essingeleden norrgående 22
		// 52300_AB --> 52300_N
		chargingPoint2link.put("88", "52300_N");

		// 95249
		// Kristineberg Avfart Essingeleden S 23
		// 95249_AB --> REMOVED: 95249_NE
		// [removed in network cleaning]

		// 52301
		// Kristineberg Påfart Essingeleden N 24
		// 52301_AB --> 52301_N
		chargingPoint2link.put("94", "52301_N");

		// 128236
		// Kristineberg Avfart Essingeleden N 25
		// [not encoded]

		// 128234
		// Kristineberg Påfart Essingeleden S 26
		// [not encoded]

		// 122017
		// Klarastrandsleden infart 27
		// Klarastrandsleden utfart 28
		// 122017_AB --> 122017_NW
		// 122017_BA --> 122017_SE
		// Klarastrandsleden infart 27 122017
		// IN chargingPoint2link.put("101", "122017_SE");
		// Klarastrandsleden utfart 28 122017
		chargingPoint2link.put("102", "122017_NW");

		// 121908
		// Ekelundsbron infart 29
		// Ekelundsbron utfart 30
		// 121908_AB --> 121908_N
		// 121908_BA --> 121908_S
		// Ekelundsbron infart 29 121908
		// IN chargingPoint2link.put("111", "121908_S");
		// Ekelundsbron utfart 30 121908
		chargingPoint2link.put("112", "121908_N");

		// 52416
		// Tomtebodavägen infart NY 31
		// Tomtebodavägen utfart NY 32
		// 52416_AB --> REMOVED: 52416_SW
		// 52416_BA --> 52416_NE
		// [problematic, unclear which of the two was cleaned]

		// 108353
		// Solnavägen infart 33
		// 108353_AB --> 108353_SE
		// IN chargingPoint2link.put("131", "108353_SE");

		// 113763
		// Solnavägen utfart 34
		// 113763_AB --> 113763_NW
		chargingPoint2link.put("132", "113763_NW");

		// 101350
		// Norrtull Sveavägen utfart 36
		// 101350_AB --> 101350_NW
		chargingPoint2link.put("146", "101350_NW");

		// 128229
		// Norrtull tillfällig väg 38
		// [not encoded]

		// 34743
		// Norra stationsgatan utfart 39
		// 34743_AB --> 34743_NE
		chargingPoint2link.put("148", "34743_NE");

		// 54215
		// Ekhagen Avfart E18S 40
		// 54215_AB --> 54215_S
		// IN chargingPoint2link.put("211", "54215_S");

		// 116809
		// Ekhagen Påfart E18N 41
		// 116809_AB --> 116809_NW
		chargingPoint2link.put("212", "116809_NW");

		// 74955
		// Ekhagen Avfart E18N 42
		// 74955_AB --> 74955_N
		// IN chargingPoint2link.put("213", "74955_N");

		// 35466
		// Ekhagen Påfart E18S 43
		// 35466_AB --> 35466_SE
		chargingPoint2link.put("214", "35466_SE");

		// 56348
		// Frescati infart 44
		// Frescati utfart 45
		// 56348_AB --> REMOVED: 56348_W
		// 56348_BA --> REMOVED: 56348_E
		// [removed when cleaning network]

		// 127555
		// Universitetet infart 46
		// [not encoded]

		// 42557
		// Universitetet utfart 47
		// 42557_AB --> 42557_N
		chargingPoint2link.put("232", "42557_N");

		// 129132
		// Roslagstull infart 48
		// 127146
		// Roslagstull utfart 49
		// [not encoded]

		// 128187
		// Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50
		// 128192
		// Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51
		// 128204
		// Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52
		// 128219
		// Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53
		// 128215
		// Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54
		// [not encoded]

		// 23370
		// Ropsten Infart till Norra Hamnvägen 55
		// 23370_AB --> 23370_NE
		// IN chargingPoint2link.put("261", "23370_NE");

		// 43117
		// Ropsten Utfart från Norra Hamnvägen 56
		// 43117_AB --> 43117_N
		chargingPoint2link.put("262", "43117_N");

		// 125185
		// Ropsten Infart mot Hjorthagen 57
		// 125185_AB --> 125167_N
		// IN chargingPoint2link.put("263", "125167_N");

		// 58297
		// Ropsten Utfart från Hjorthagen 58
		// 58297_AB --> 58297_SE
		chargingPoint2link.put("264", "58297_SE");

		return chargingPoint2link;
	}

	private static Map<String, String> chargingPoint2link() {
		final Map<String, String> chargingPoint2link = new LinkedHashMap<>();
		chargingPoint2link.putAll(ingoingChargingPoint2link());
		chargingPoint2link.putAll(outgoingChargingPoint2link());
		return chargingPoint2link;
	}

}
