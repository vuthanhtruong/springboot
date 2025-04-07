const countrySelect = document.getElementById("country");
const provinceSelect = document.getElementById("province");
const districtSelect = document.getElementById("district");
const wardSelect = document.getElementById("ward");

// Tải danh sách quốc gia từ Rest Countries API
axios.get("https://restcountries.com/v3.1/all", {timeout: 5000})
    .then(response => {
        const countries = response.data.sort((a, b) => a.name.common.localeCompare(b.name.common));
        countrySelect.innerHTML = '<option value="">Chọn quốc gia</option>'; // Reset danh sách
        countries.forEach(country => {
            const option = document.createElement("option");
            option.value = country.cca2;
            option.textContent = country.name.common;
            countrySelect.appendChild(option);
        });
    })
    .catch(error => {
        console.error("Lỗi tải quốc gia:", error.message);
        countrySelect.innerHTML = '<option value="">Không thể tải quốc gia</option>';
    });

// Tải danh sách tỉnh/thành phố khi chọn quốc gia
countrySelect.addEventListener("change", function () {
    const countryCode = this.value;
    provinceSelect.innerHTML = '<option value="">Chọn tỉnh/thành phố</option>';
    districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn xã/phường</option>';

    if (countryCode) {
        const geonameId = getCountryGeonameId(countryCode);
        if (!geonameId) {
            provinceSelect.innerHTML = '<option value="">Quốc gia này chưa được hỗ trợ</option>';
            return;
        }

        // Gọi qua endpoint proxy của Spring Boot thay vì trực tiếp
        const url = `/api/geonames/children?geonameId=${geonameId}`;
        console.log("Gửi yêu cầu tới:", url);

        axios.get(url)
            .then(response => {
                console.log("Dữ liệu trả về:", response.data);
                const provinces = response.data.geonames || [];
                if (!provinces.length) {
                    provinceSelect.innerHTML = '<option value="">Không có dữ liệu tỉnh/thành phố</option>';
                } else {
                    provinces.forEach(province => {
                        const option = document.createElement("option");
                        option.value = province.name;
                        option.textContent = province.name;
                        option.dataset.geonameId = province.geonameId;
                        provinceSelect.appendChild(option);
                    });
                }
            })
            .catch(error => {
                const errorMessage = error.response?.data?.status?.message || error.message;
                console.error("Lỗi tải tỉnh/thành phố:", errorMessage);
                provinceSelect.innerHTML = `<option value="">Lỗi: ${errorMessage}</option>`;
            });
    }
});

// Tải danh sách quận/huyện khi chọn tỉnh/thành phố
provinceSelect.addEventListener("change", function () {
    const selectedOption = this.options[this.selectedIndex];
    const provinceGeonameId = selectedOption?.dataset.geonameId;
    districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn xã/phường</option>';

    if (provinceGeonameId) {
        const url = `/api/geonames/children?geonameId=${provinceGeonameId}`;
        console.log("Gửi yêu cầu tới:", url);

        axios.get(url)
            .then(response => {
                console.log("Dữ liệu trả về:", response.data);
                const districts = response.data.geonames || [];
                if (!districts.length) {
                    districtSelect.innerHTML = '<option value="">Không có dữ liệu quận/huyện</option>';
                } else {
                    districts.forEach(district => {
                        const option = document.createElement("option");
                        option.value = district.name;
                        option.textContent = district.name;
                        option.dataset.geonameId = district.geonameId;
                        districtSelect.appendChild(option);
                    });
                }
            })
            .catch(error => {
                const errorMessage = error.response?.data?.status?.message || error.message;
                console.error("Lỗi tải quận/huyện:", errorMessage);
                districtSelect.innerHTML = `<option value="">Lỗi: ${errorMessage}</option>`;
            });
    }
});

// Tải danh sách xã/phường khi chọn quận/huyện
districtSelect.addEventListener("change", function () {
    const selectedOption = this.options[this.selectedIndex];
    const districtGeonameId = selectedOption?.dataset.geonameId;
    wardSelect.innerHTML = '<option value="">Chọn xã/phường</option>';

    if (districtGeonameId) {
        const url = `/api/geonames/children?geonameId=${districtGeonameId}`;
        console.log("Gửi yêu cầu tới:", url);

        axios.get(url)
            .then(response => {
                console.log("Dữ liệu trả về:", response.data);
                const wards = response.data.geonames || [];
                if (!wards.length) {
                    wardSelect.innerHTML = '<option value="">Không có dữ liệu xã/phường</option>';
                } else {
                    wards.forEach(ward => {
                        const option = document.createElement("option");
                        option.value = ward.name;
                        option.textContent = ward.name;
                        wardSelect.appendChild(option);
                    });
                }
            })
            .catch(error => {
                const errorMessage = error.response?.data?.status?.message || error.message;
                console.error("Lỗi tải xã/phường:", errorMessage);
                wardSelect.innerHTML = `<option value="">Lỗi: ${errorMessage}</option>`;
            });
    }
});

// Hàm lấy geonameId của quốc gia (giữ nguyên)
function getCountryGeonameId(countryCode) {
    const countryIds = {
        "AF": 1149361, "AL": 783754, "DZ": 2589581, "AD": 3041565, "AO": 3351879, "AG": 3576396,
        "AR": 3865483, "AM": 174982, "AU": 2077456, "AT": 2782113, "AZ": 587116, "BS": 3572887,
        "BH": 290291, "BD": 1210997, "BB": 3374084, "BY": 630336, "BE": 2802361, "BZ": 3582678,
        "BJ": 2395170, "BT": 1252634, "BO": 3923057, "BA": 3277605, "BW": 933860, "BR": 3469034,
        "BN": 1820814, "BG": 732800, "BF": 2361809, "BI": 433561, "KH": 1831722, "CM": 2233387,
        "CA": 6251999, "CV": 3374766, "CF": 239880, "TD": 2434508, "CL": 3895114, "CN": 1814991,
        "CO": 3686110, "KM": 921929, "CD": 203312, "CG": 2260494, "CR": 3624060, "HR": 3202326,
        "CU": 3562981, "CY": 146669, "CZ": 3077311, "DK": 2623032, "DJ": 223816, "DO": 3508796,
        "EC": 3658394, "EG": 357994, "SV": 3585968, "GQ": 2309096, "ER": 338010, "EE": 453733,
        "ET": 337996, "FJ": 2205218, "FI": 660013, "FR": 3017382, "GA": 2400553, "GM": 2413451,
        "GE": 614540, "DE": 2921044, "GH": 2300660, "GR": 390903, "GT": 3595528, "GN": 2420477,
        "GY": 3378535, "HT": 3723988, "HN": 3608932, "HU": 719819, "IS": 2629691, "IN": 1269750,
        "ID": 1643084, "IR": 130758, "IQ": 99237, "IE": 2963597, "IT": 3175395, "JP": 1861060,
        "JO": 248816, "KZ": 1522867, "KE": 192950, "KR": 1835841, "KW": 285570, "KG": 1527747,
        "LA": 1655842, "LV": 458258, "LB": 272103, "LY": 2215636, "LT": 597427, "LU": 2960313,
        "MG": 1062947, "MW": 927384, "MY": 1733045, "MV": 1282028, "ML": 2453866, "MT": 2562770,
        "MX": 3996063, "MD": 617790, "MC": 2993457, "MN": 2029969, "MA": 2542007, "MZ": 1036973,
        "MM": 1327865, "NA": 3355338, "NP": 1282988, "NL": 2750405, "NZ": 2186224, "NG": 2328926,
        "NO": 3144096, "OM": 286963, "PK": 1168579, "PA": 3703430, "PY": 3437598, "PE": 3932488,
        "PH": 1694008, "PL": 798544, "PT": 2264397, "QA": 289688, "RO": 798549, "RU": 2017370,
        "SA": 102358, "SG": 1880251, "ZA": 953987, "ES": 2510769, "SE": 2661886, "CH": 2658434,
        "TH": 1605651, "TR": 298795, "UA": 690791, "AE": 290557, "GB": 2635167, "US": 6252001,
        "VN": 1562822, "ZW": 878675, "BQ": 7626844, "SS": 7909807, "TL": 1966436, "SX": 7609695,
        "XK": 831053, "HK": 1819730, "MO": 1821275, "FO": 2622320, "GG": 3042362, "IM": 3042225,
        "JE": 3042142, "PF": 4030656, "NC": 2139685, "PM": 3424932, "WF": 4034749, "KI": 4030945,
        "MH": 2080185, "FM": 2081918, "NR": 2110425, "PW": 1559582, "WS": 4034894, "SM": 3168068,
        "ST": 2410758, "SC": 241170, "TV": 2110297, "VA": 3164670, "AI": 3573511, "BM": 3573345,
        "IO": 1282588, "VG": 3577718, "KY": 3580718, "FK": 3474414, "GI": 2411586, "MS": 3578097,
        "SH": 3370751, "TC": 3576916, "MP": 4041468, "GU": 4043988, "AS": 5880801, "VI": 4796775,
        "RE": 935317, "YT": 1024031, "GP": 3579143, "MF": 3578421, "BL": 3578476, "CW": 7626836,
        "AW": 3577279
    };
    return countryIds[countryCode] || null;
}