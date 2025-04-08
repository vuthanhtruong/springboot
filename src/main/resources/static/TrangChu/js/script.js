const countrySelect = document.getElementById("country");
const provinceSelect = document.getElementById("province");
const districtSelect = document.getElementById("district");
const wardSelect = document.getElementById("ward");

// Hàm thử lại yêu cầu API
async function fetchWithRetry(url, retries = 3, delay = 1000) {
    for (let i = 0; i < retries; i++) {
        try {
            const response = await axios.get(url);
            return response;
        } catch (error) {
            if (i < retries - 1) {
                console.log(`Thử lại lần ${i + 1} sau ${delay}ms...`);
                await new Promise(resolve => setTimeout(resolve, delay));
                continue;
            }
            throw error;
        }
    }
}

// Tải danh sách quốc gia từ GeoNames API
const geoNamesUsername = "vuthanhtruong";
fetchWithRetry(`http://api.geonames.org/countryInfoJSON?username=${geoNamesUsername}`)
    .then(response => {
        console.log("Dữ liệu quốc gia:", response.data);
        const countries = response.data.geonames.sort((a, b) => a.countryName.localeCompare(b.countryName));
        populateCountries(countries);
    })
    .catch(error => {
        console.error("Lỗi tải quốc gia:", error);
        console.log("Dùng dữ liệu dự phòng...");
        const fallbackCountries = getFallbackCountries();
        populateCountries(fallbackCountries);
    });

// Hàm điền danh sách quốc gia với Select2
function populateCountries(countries) {
    // Điền dữ liệu vào select
    countrySelect.innerHTML = '<option value="">Chọn quốc gia</option>';
    countries.forEach(country => {
        const option = document.createElement("option");
        option.value = country.countryCode;
        option.textContent = country.countryName;
        option.dataset.geonameId = country.geonameId;
        option.dataset.flag = `https://flagcdn.com/24x18/${country.countryCode.toLowerCase()}.png`; // URL hình cờ
        countrySelect.appendChild(option);
    });

    // Khởi tạo Select2 với template tùy chỉnh
    $(countrySelect).select2({
        templateResult: formatCountry, // Hiển thị cờ trong danh sách dropdown
        templateSelection: formatCountry, // Hiển thị cờ khi chọn
    });
}

// Hàm định dạng option với cờ
function formatCountry(state) {
    if (!state.id) {
        return state.text; // Trả về text mặc định cho placeholder
    }
    const flagUrl = $(state.element).data('flag');
    const $state = $(
        `<span><img src="${flagUrl}" class="flag" style="width: 24px; height: 18px; margin-right: 8px;" />${state.text}</span>`
    );
    return $state;
}

$(countrySelect).on('select2:select', function (e) {
    const countryCode = e.params.data.id; // Lấy countryCode từ lựa chọn
    const selectedOption = e.params.data.element;
    const geonameId = selectedOption.dataset.geonameId;
    console.log("Quốc gia được chọn:", countryCode, "GeonameId:", geonameId);
    provinceSelect.innerHTML = '<option value="">Chọn tỉnh/thành phố</option>';
    districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn xã/phường</option>';

    if (geonameId) {
        const url = `http://api.geonames.org/childrenJSON?geonameId=${geonameId}&username=${geoNamesUsername}&maxRows=1000`;
        console.log("Gửi yêu cầu tới:", url);

        axios.get(url)
            .then(response => {
                console.log("Dữ liệu tỉnh/thành phố:", response.data);
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
                const errorMessage = error.response
                    ? (error.response.data.status ? error.response.data.status.message : error.response.data)
                    : error.message;
                console.error("Lỗi tải tỉnh/thành phố:", errorMessage);
                provinceSelect.innerHTML = `<option value="">Lỗi: ${errorMessage}</option>`;
            });
    }
});

// Tải danh sách quận/huyện khi chọn tỉnh/thành phố
provinceSelect.addEventListener("change", function () {
    const selectedOption = this.options[this.selectedIndex];
    const provinceGeonameId = selectedOption ? selectedOption.dataset.geonameId : null;
    districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn xã/phường</option>';

    if (provinceGeonameId) {
        const url = `http://api.geonames.org/childrenJSON?geonameId=${provinceGeonameId}&username=${geoNamesUsername}&maxRows=1000`;
        console.log("Gửi yêu cầu tới:", url);

        axios.get(url)
            .then(response => {
                console.log("Dữ liệu quận/huyện:", response.data);
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
                const errorMessage = error.response
                    ? (error.response.data.status ? error.response.data.status.message : error.response.data)
                    : error.message;
                console.error("Lỗi tải quận/huyện:", errorMessage);
                districtSelect.innerHTML = `<option value="">Lỗi: ${errorMessage}</option>`;
            });
    }
});

// Tải danh sách xã/phường khi chọn quận/huyện
districtSelect.addEventListener("change", function () {
    const selectedOption = this.options[this.selectedIndex];
    const districtGeonameId = selectedOption ? selectedOption.dataset.geonameId : null;
    wardSelect.innerHTML = '<option value="">Chọn xã/phường</option>';

    if (districtGeonameId) {
        const url = `http://api.geonames.org/childrenJSON?geonameId=${districtGeonameId}&username=${geoNamesUsername}&maxRows=1000`;
        console.log("Gửi yêu cầu tới:", url);

        axios.get(url)
            .then(response => {
                console.log("Dữ liệu xã/phường:", response.data);
                const wards = response.data.geonames || [];
                if (!wards.length) {
                    wardSelect.innerHTML = '<option value="">Không có dữ liệu xã/phường</option>';
                } else {
                    wards.forEach(ward => {
                        const option = document.createElement("option");
                        option.value = ward.name;
                        option.textContent = ward.name;
                        option.dataset.geonameId = ward.geonameId;
                        wardSelect.appendChild(option);
                    });
                }
            })
            .catch(error => {
                const errorMessage = error.response
                    ? (error.response.data.status ? error.response.data.status.message : error.response.data)
                    : error.message;
                console.error("Lỗi tải xã/phường:", errorMessage);
                wardSelect.innerHTML = `<option value="">Lỗi: ${errorMessage}</option>`;
            });
    }
});

// Hàm lấy dữ liệu quốc gia dự phòng (toàn bộ quốc gia)
function getFallbackCountries() {
    return [
        {countryCode: "AF", countryName: "Afghanistan", geonameId: 1149361},
        {countryCode: "AL", countryName: "Albania", geonameId: 783754},
        {countryCode: "DZ", countryName: "Algeria", geonameId: 2589581},
        {countryCode: "AD", countryName: "Andorra", geonameId: 3041565},
        {countryCode: "AO", countryName: "Angola", geonameId: 3351879},
        {countryCode: "AG", countryName: "Antigua and Barbuda", geonameId: 3576396},
        {countryCode: "AR", countryName: "Argentina", geonameId: 3865483},
        {countryCode: "AM", countryName: "Armenia", geonameId: 174982},
        {countryCode: "AU", countryName: "Australia", geonameId: 2077456},
        {countryCode: "AT", countryName: "Austria", geonameId: 2782113},
        {countryCode: "AZ", countryName: "Azerbaijan", geonameId: 587116},
        {countryCode: "BS", countryName: "Bahamas", geonameId: 3572887},
        {countryCode: "BH", countryName: "Bahrain", geonameId: 290291},
        {countryCode: "BD", countryName: "Bangladesh", geonameId: 1210997},
        {countryCode: "BB", countryName: "Barbados", geonameId: 3374084},
        {countryCode: "BY", countryName: "Belarus", geonameId: 630336},
        {countryCode: "BE", countryName: "Belgium", geonameId: 2802361},
        {countryCode: "BZ", countryName: "Belize", geonameId: 3582678},
        {countryCode: "BJ", countryName: "Benin", geonameId: 2395170},
        {countryCode: "BT", countryName: "Bhutan", geonameId: 1252634},
        {countryCode: "BO", countryName: "Bolivia", geonameId: 3923057},
        {countryCode: "BA", countryName: "Bosnia and Herzegovina", geonameId: 3277605},
        {countryCode: "BW", countryName: "Botswana", geonameId: 933860},
        {countryCode: "BR", countryName: "Brazil", geonameId: 3469034},
        {countryCode: "BN", countryName: "Brunei", geonameId: 1820814},
        {countryCode: "BG", countryName: "Bulgaria", geonameId: 732800},
        {countryCode: "BF", countryName: "Burkina Faso", geonameId: 2361809},
        {countryCode: "BI", countryName: "Burundi", geonameId: 433561},
        {countryCode: "KH", countryName: "Cambodia", geonameId: 1831722},
        {countryCode: "CM", countryName: "Cameroon", geonameId: 2233387},
        {countryCode: "CA", countryName: "Canada", geonameId: 6251999},
        {countryCode: "CV", countryName: "Cape Verde", geonameId: 3374766},
        {countryCode: "CF", countryName: "Central African Republic", geonameId: 239880},
        {countryCode: "TD", countryName: "Chad", geonameId: 2434508},
        {countryCode: "CL", countryName: "Chile", geonameId: 3895114},
        {countryCode: "CN", countryName: "China", geonameId: 1814991},
        {countryCode: "CO", countryName: "Colombia", geonameId: 3686110},
        {countryCode: "KM", countryName: "Comoros", geonameId: 921929},
        {countryCode: "CD", countryName: "Congo (DRC)", geonameId: 203312},
        {countryCode: "CG", countryName: "Congo (Republic)", geonameId: 2260494},
        {countryCode: "CR", countryName: "Costa Rica", geonameId: 3624060},
        {countryCode: "HR", countryName: "Croatia", geonameId: 3202326},
        {countryCode: "CU", countryName: "Cuba", geonameId: 3562981},
        {countryCode: "CY", countryName: "Cyprus", geonameId: 146669},
        {countryCode: "CZ", countryName: "Czechia", geonameId: 3077311},
        {countryCode: "DK", countryName: "Denmark", geonameId: 2623032},
        {countryCode: "DJ", countryName: "Djibouti", geonameId: 223816},
        {countryCode: "DM", countryName: "Dominica", geonameId: 3575830},
        {countryCode: "DO", countryName: "Dominican Republic", geonameId: 3508796},
        {countryCode: "EC", countryName: "Ecuador", geonameId: 3658394},
        {countryCode: "EG", countryName: "Egypt", geonameId: 357994},
        {countryCode: "SV", countryName: "El Salvador", geonameId: 3585968},
        {countryCode: "GQ", countryName: "Equatorial Guinea", geonameId: 2309096},
        {countryCode: "ER", countryName: "Eritrea", geonameId: 338010},
        {countryCode: "EE", countryName: "Estonia", geonameId: 453733},
        {countryCode: "SZ", countryName: "Eswatini", geonameId: 934841},
        {countryCode: "ET", countryName: "Ethiopia", geonameId: 337996},
        {countryCode: "FJ", countryName: "Fiji", geonameId: 2205218},
        {countryCode: "FI", countryName: "Finland", geonameId: 660013},
        {countryCode: "FR", countryName: "France", geonameId: 3017382},
        {countryCode: "GA", countryName: "Gabon", geonameId: 2400553},
        {countryCode: "GM", countryName: "Gambia", geonameId: 2413451},
        {countryCode: "GE", countryName: "Georgia", geonameId: 614540},
        {countryCode: "DE", countryName: "Germany", geonameId: 2921044},
        {countryCode: "GH", countryName: "Ghana", geonameId: 2300660},
        {countryCode: "GR", countryName: "Greece", geonameId: 390903},
        {countryCode: "GD", countryName: "Grenada", geonameId: 3580239},
        {countryCode: "GT", countryName: "Guatemala", geonameId: 3595528},
        {countryCode: "GN", countryName: "Guinea", geonameId: 2420477},
        {countryCode: "GW", countryName: "Guinea-Bissau", geonameId: 2372248},
        {countryCode: "GY", countryName: "Guyana", geonameId: 3378535},
        {countryCode: "HT", countryName: "Haiti", geonameId: 3723988},
        {countryCode: "HN", countryName: "Honduras", geonameId: 3608932},
        {countryCode: "HU", countryName: "Hungary", geonameId: 719819},
        {countryCode: "IS", countryName: "Iceland", geonameId: 2629691},
        {countryCode: "IN", countryName: "India", geonameId: 1269750},
        {countryCode: "ID", countryName: "Indonesia", geonameId: 1643084},
        {countryCode: "IR", countryName: "Iran", geonameId: 130758},
        {countryCode: "IQ", countryName: "Iraq", geonameId: 99237},
        {countryCode: "IE", countryName: "Ireland", geonameId: 2963597},
        {countryCode: "IL", countryName: "Israel", geonameId: 294640},
        {countryCode: "IT", countryName: "Italy", geonameId: 3175395},
        {countryCode: "JM", countryName: "Jamaica", geonameId: 3489940},
        {countryCode: "JP", countryName: "Japan", geonameId: 1861060},
        {countryCode: "JO", countryName: "Jordan", geonameId: 248816},
        {countryCode: "KZ", countryName: "Kazakhstan", geonameId: 1522867},
        {countryCode: "KE", countryName: "Kenya", geonameId: 192950},
        {countryCode: "KI", countryName: "Kiribati", geonameId: 4030945},
        {countryCode: "KP", countryName: "North Korea", geonameId: 1873107},
        {countryCode: "KR", countryName: "South Korea", geonameId: 1835841},
        {countryCode: "KW", countryName: "Kuwait", geonameId: 285570},
        {countryCode: "KG", countryName: "Kyrgyzstan", geonameId: 1527747},
        {countryCode: "LA", countryName: "Laos", geonameId: 1655842},
        {countryCode: "LV", countryName: "Latvia", geonameId: 458258},
        {countryCode: "LB", countryName: "Lebanon", geonameId: 272103},
        {countryCode: "LS", countryName: "Lesotho", geonameId: 932692},
        {countryCode: "LR", countryName: "Liberia", geonameId: 2275384},
        {countryCode: "LY", countryName: "Libya", geonameId: 2215636},
        {countryCode: "LI", countryName: "Liechtenstein", geonameId: 3042058},
        {countryCode: "LT", countryName: "Lithuania", geonameId: 597427},
        {countryCode: "LU", countryName: "Luxembourg", geonameId: 2960313},
        {countryCode: "MG", countryName: "Madagascar", geonameId: 1062947},
        {countryCode: "MW", countryName: "Malawi", geonameId: 927384},
        {countryCode: "MY", countryName: "Malaysia", geonameId: 1733045},
        {countryCode: "MV", countryName: "Maldives", geonameId: 1282028},
        {countryCode: "ML", countryName: "Mali", geonameId: 2453866},
        {countryCode: "MT", countryName: "Malta", geonameId: 2562770},
        {countryCode: "MH", countryName: "Marshall Islands", geonameId: 2080185},
        {countryCode: "MR", countryName: "Mauritania", geonameId: 2378080},
        {countryCode: "MU", countryName: "Mauritius", geonameId: 934154},
        {countryCode: "MX", countryName: "Mexico", geonameId: 3996063},
        {countryCode: "FM", countryName: "Micronesia", geonameId: 2081918},
        {countryCode: "MD", countryName: "Moldova", geonameId: 617790},
        {countryCode: "MC", countryName: "Monaco", geonameId: 2993457},
        {countryCode: "MN", countryName: "Mongolia", geonameId: 2029969},
        {countryCode: "ME", countryName: "Montenegro", geonameId: 3194884},
        {countryCode: "MA", countryName: "Morocco", geonameId: 2542007},
        {countryCode: "MZ", countryName: "Mozambique", geonameId: 1036973},
        {countryCode: "MM", countryName: "Myanmar", geonameId: 1327865},
        {countryCode: "NA", countryName: "Namibia", geonameId: 3355338},
        {countryCode: "NR", countryName: "Nauru", geonameId: 2110425},
        {countryCode: "NP", countryName: "Nepal", geonameId: 1282988},
        {countryCode: "NL", countryName: "Netherlands", geonameId: 2750405},
        {countryCode: "NZ", countryName: "New Zealand", geonameId: 2186224},
        {countryCode: "NI", countryName: "Nicaragua", geonameId: 3617476},
        {countryCode: "NE", countryName: "Niger", geonameId: 2440476},
        {countryCode: "NG", countryName: "Nigeria", geonameId: 2328926},
        {countryCode: "NO", countryName: "Norway", geonameId: 3144096},
        {countryCode: "OM", countryName: "Oman", geonameId: 286963},
        {countryCode: "PK", countryName: "Pakistan", geonameId: 1168579},
        {countryCode: "PW", countryName: "Palau", geonameId: 1559582},
        {countryCode: "PA", countryName: "Panama", geonameId: 3703430},
        {countryCode: "PG", countryName: "Papua New Guinea", geonameId: 2088628},
        {countryCode: "PY", countryName: "Paraguay", geonameId: 3437598},
        {countryCode: "PE", countryName: "Peru", geonameId: 3932488},
        {countryCode: "PH", countryName: "Philippines", geonameId: 1694008},
        {countryCode: "PL", countryName: "Poland", geonameId: 798544},
        {countryCode: "PT", countryName: "Portugal", geonameId: 2264397},
        {countryCode: "QA", countryName: "Qatar", geonameId: 289688},
        {countryCode: "RO", countryName: "Romania", geonameId: 798549},
        {countryCode: "RU", countryName: "Russia", geonameId: 2017370},
        {countryCode: "RW", countryName: "Rwanda", geonameId: 49518},
        {countryCode: "KN", countryName: "Saint Kitts and Nevis", geonameId: 3575174},
        {countryCode: "LC", countryName: "Saint Lucia", geonameId: 3576468},
        {countryCode: "VC", countryName: "Saint Vincent and the Grenadines", geonameId: 3577815},
        {countryCode: "WS", countryName: "Samoa", geonameId: 4034894},
        {countryCode: "SM", countryName: "San Marino", geonameId: 3168068},
        {countryCode: "ST", countryName: "Sao Tome and Principe", geonameId: 2410758},
        {countryCode: "SA", countryName: "Saudi Arabia", geonameId: 102358},
        {countryCode: "SN", countryName: "Senegal", geonameId: 2245662},
        {countryCode: "RS", countryName: "Serbia", geonameId: 6290252},
        {countryCode: "SC", countryName: "Seychelles", geonameId: 241170},
        {countryCode: "SL", countryName: "Sierra Leone", geonameId: 2403846},
        {countryCode: "SG", countryName: "Singapore", geonameId: 1880251},
        {countryCode: "SK", countryName: "Slovakia", geonameId: 3057568},
        {countryCode: "SI", countryName: "Slovenia", geonameId: 3190538},
        {countryCode: "SB", countryName: "Solomon Islands", geonameId: 2103350},
        {countryCode: "SO", countryName: "Somalia", geonameId: 51537},
        {countryCode: "ZA", countryName: "South Africa", geonameId: 953987},
        {countryCode: "SS", countryName: "South Sudan", geonameId: 7909807},
        {countryCode: "ES", countryName: "Spain", geonameId: 2510769},
        {countryCode: "LK", countryName: "Sri Lanka", geonameId: 1227603},
        {countryCode: "SD", countryName: "Sudan", geonameId: 366755},
        {countryCode: "SR", countryName: "Suriname", geonameId: 3382998},
        {countryCode: "SE", countryName: "Sweden", geonameId: 2661886},
        {countryCode: "CH", countryName: "Switzerland", geonameId: 2658434},
        {countryCode: "SY", countryName: "Syria", geonameId: 163843},
        {countryCode: "TW", countryName: "Taiwan", geonameId: 1668284},
        {countryCode: "TJ", countryName: "Tajikistan", geonameId: 1220409},
        {countryCode: "TZ", countryName: "Tanzania", geonameId: 149590},
        {countryCode: "TH", countryName: "Thailand", geonameId: 1605651},
        {countryCode: "TL", countryName: "Timor-Leste", geonameId: 1966436},
        {countryCode: "TG", countryName: "Togo", geonameId: 2363686},
        {countryCode: "TO", countryName: "Tonga", geonameId: 4032283},
        {countryCode: "TT", countryName: "Trinidad and Tobago", geonameId: 3573591},
        {countryCode: "TN", countryName: "Tunisia", geonameId: 2464461},
        {countryCode: "TR", countryName: "Turkey", geonameId: 298795},
        {countryCode: "TM", countryName: "Turkmenistan", geonameId: 1218197},
        {countryCode: "TV", countryName: "Tuvalu", geonameId: 2110297},
        {countryCode: "UG", countryName: "Uganda", geonameId: 226074},
        {countryCode: "UA", countryName: "Ukraine", geonameId: 690791},
        {countryCode: "AE", countryName: "United Arab Emirates", geonameId: 290557},
        {countryCode: "GB", countryName: "United Kingdom", geonameId: 2635167},
        {countryCode: "US", countryName: "United States", geonameId: 6252001},
        {countryCode: "UY", countryName: "Uruguay", geonameId: 3439705},
        {countryCode: "UZ", countryName: "Uzbekistan", geonameId: 1512440},
        {countryCode: "VU", countryName: "Vanuatu", geonameId: 2134431},
        {countryCode: "VA", countryName: "Vatican City", geonameId: 3164670},
        {countryCode: "VE", countryName: "Venezuela", geonameId: 3625428},
        {countryCode: "VN", countryName: "Vietnam", geonameId: 1562822},
        {countryCode: "YE", countryName: "Yemen", geonameId: 69543},
        {countryCode: "ZM", countryName: "Zambia", geonameId: 895949},
        {countryCode: "ZW", countryName: "Zimbabwe", geonameId: 878675}
    ];
}