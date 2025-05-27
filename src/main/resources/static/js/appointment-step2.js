document.addEventListener('DOMContentLoaded', function() {
    // Elementos del DOM
    const calendarTitleEl = document.getElementById('calendarTitle');
    const calendarDaysGridEl = document.getElementById('calendarDaysGrid');
    const prevMonthBtn = document.getElementById('prevMonthBtn');
    const nextMonthBtn = document.getElementById('nextMonthBtn');
    const selectedDateTitleEl = document.getElementById('selectedDateTitle');
    const timeSlotsGridEl = document.getElementById('timeSlotsGrid');
    const noSlotsMessageEl = document.getElementById('noSlotsMessage');
    const slotsLoaderEl = document.getElementById('slotsLoader');

    const selectSlotForm = document.getElementById('selectSlotForm'); // El formulario
    const selectedDateInput = document.getElementById('selectedDateInput');
    const selectedTimeInput = document.getElementById('selectedTimeInput');
    const selectedDoctorIdInput = document.getElementById('selectedDoctorIdInput'); // Si filtras por doctor
    const nextStepBtn = document.getElementById('nextStepBtn');
    // const doctorFilter = document.getElementById('doctorFilter'); // Si tienes filtro de médico

    // Datos iniciales pasados desde Thymeleaf (asegúrate que estas variables globales sean definidas en tu HTML)
    const initialAvailableDates = /*[[${availableDates}]]*/ []; // Lista de Strings "YYYY-MM-DD"
    // const initialYearMonth;    // String "YYYY-MM"
    // const specialtyId;         // Integer
    // const csrfHeaderName;      // String
    // const csrfToken;           // String

    let currentDisplayedCalendarDate; // Objeto Date de JS para el mes/año actual del calendario
    let availableDatesInCurrentMonthSet = new Set(initialAvailableDates || []);
    let selectedFullDate = null; // String "YYYY-MM-DD"
    let selectedTimeSlot = null; // String "HH:MM" (o el formato que envíe la API)
    let selectedDoctorForSlot = null; // Long (ID del médico para el slot seleccionado)

    console.log("Fechas disponibles iniciales recibidas:", initialAvailableDates);
    console.log("Mes/Año inicial:", initialYearMonth);
    console.log("ID Especialidad:", specialtyId);

    // Configuración del local para date-fns (español)
    const dateFnsLocale = (dateFns.locale && dateFns.locale.es) ? { locale: dateFns.locale.es } : undefined;
    if (!dateFnsLocale) {
        console.warn("date-fns: Local 'es' no encontrado. Se usará el formato de fecha por defecto (inglés).");
    }

    // --- INICIALIZACIÓN ---
    function initializeCalendar() {
        if (initialYearMonth) {
            const [year, month] = initialYearMonth.split('-').map(Number);
            currentDisplayedCalendarDate = new Date(year, month - 1, 1); // Mes en JS Date es 0-indexado
        } else {
            currentDisplayedCalendarDate = dateFns.startOfMonth(new Date());
            console.warn("InitialYearMonth no proporcionado, usando mes actual.");
        }
        renderCalendar();
        updateSelectedDateTitle(null); // Limpiar título de fecha seleccionada
        noSlotsMessageEl.textContent = "Selecciona una fecha con disponibilidad para ver los horarios.";
        noSlotsMessageEl.style.display = 'block';
    }

    // --- LÓGICA DEL CALENDARIO ---
    function renderCalendar() {
        calendarTitleEl.textContent = dateFns.format(currentDisplayedCalendarDate, 'MMMM yyyy', dateFnsLocale);
        calendarDaysGridEl.innerHTML = ''; // Limpiar días anteriores

        const year = currentDisplayedCalendarDate.getFullYear();
        const month = currentDisplayedCalendarDate.getMonth(); // 0-indexado

        const firstDayOfMonthDate = dateFns.startOfMonth(currentDisplayedCalendarDate);
        const firstDayOfWeekIndex = dateFns.getDay(firstDayOfMonthDate); // 0 (Dom) a 6 (Sáb) para date-fns
        const daysInMonth = dateFns.getDaysInMonth(currentDisplayedCalendarDate);
        const todayStr = dateFns.format(new Date(), 'yyyy-MM-dd');

        // Días vacíos al inicio del mes para alinear la semana (asumiendo que el grid visual empieza en Domingo)
        for (let i = 0; i < firstDayOfWeekIndex; i++) {
            calendarDaysGridEl.insertAdjacentHTML('beforeend', `<div class="p-1 text-center border calendar-day-empty" style="width: 14.28%; height: 50px;"></div>`);
        }

        // Días del mes
        for (let day = 1; day <= daysInMonth; day++) {
            const loopDate = new Date(year, month, day);
            const dateStr = dateFns.format(loopDate, 'yyyy-MM-dd');

            const dayEl = document.createElement('div');
            dayEl.className = 'p-1 text-center border calendar-day';
            dayEl.style.width = '14.28%';
            dayEl.style.height = '50px'; // Ajusta según tu CSS
            dayEl.textContent = day;
            dayEl.dataset.date = dateStr;

            if (dateFns.isPast(loopDate) && !dateFns.isToday(loopDate)) {
                dayEl.classList.add('disabled', 'text-muted');
            } else if (availableDatesInCurrentMonthSet.has(dateStr)) {
                dayEl.classList.add('available');
                dayEl.addEventListener('click', function() {
                    handleDateSelection(this);
                });
            } else {
                dayEl.classList.add('disabled', 'text-muted');
            }

            if (dateStr === todayStr) {
                dayEl.classList.add('today', 'fw-bold'); // Clase 'today' y negrita
            }
            if (dateStr === selectedFullDate) {
                dayEl.classList.add('selected'); // Mantener selección si se re-renderiza
            }
            calendarDaysGridEl.appendChild(dayEl);
        }
    }

    function handleDateSelection(dayElement) {
        if (selectedFullDate === dayElement.dataset.date && timeSlotsGridEl.children.length > 1) { // Si ya hay slots y se clickea el mismo día
            return; // No hacer nada si se vuelve a seleccionar el mismo día y ya hay slots
        }

        document.querySelectorAll('.calendar-day.selected').forEach(el => el.classList.remove('selected'));
        dayElement.classList.add('selected');
        selectedFullDate = dayElement.dataset.date;
        selectedDateInput.value = selectedFullDate;

        updateSelectedDateTitle(dateFns.parseISO(selectedFullDate));
        fetchTimeSlots(selectedFullDate);
    }

    function updateSelectedDateTitle(dateObject) {
        if (dateObject) {
            selectedDateTitleEl.textContent = ` ${dateFns.format(dateObject, "d 'de' MMMM 'de' yyyy", dateFnsLocale)}`;
        } else {
            selectedDateTitleEl.textContent = '-- de --';
        }
    }


    // --- LÓGICA DE SLOTS DE HORARIO ---
    function fetchTimeSlots(dateStr) {
        timeSlotsGridEl.innerHTML = '';
        noSlotsMessageEl.style.display = 'none';
        slotsLoaderEl.style.display = 'block';
        disableNextButton();
        selectedTimeSlot = null;
        selectedTimeInput.value = '';
        selectedDoctorForSlot = null;
        selectedDoctorIdInput.value = '';

        // const doctorIdParam = doctorFilter ? (doctorFilter.value ? `&doctorId=${doctorFilter.value}` : '') : ''; // Si tienes filtro de médico
        const doctorIdParam = ''; // Por ahora sin filtro de médico

        fetch(`/medimicita/api/patient/appointments/available-slots?specialtyId=${specialtyId}&date=${dateStr}${doctorIdParam}`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' } // No se necesita CSRF token para GET
        })
            .then(response => {
                slotsLoaderEl.style.display = 'none';
                if (!response.ok) {
                    return response.text().then(text => { throw new Error('Error al cargar horarios: ' + response.status + " " + text) });
                }
                return response.json();
            })
            .then(slots => {
                renderTimeSlots(slots);
            })
            .catch(error => {
                console.error('Error fetching time slots:', error);
                timeSlotsGridEl.innerHTML = `<p class="text-danger">Error al cargar horarios para el ${dateStr}. Intente de nuevo o seleccione otra fecha.</p>`;
                noSlotsMessageEl.style.display = 'none';
            });
    }

    function renderTimeSlots(slots) {
        timeSlotsGridEl.innerHTML = ''; // Limpiar slots antiguos
        if (!slots || slots.length === 0) {
            noSlotsMessageEl.textContent = 'No hay horarios disponibles para la fecha seleccionada.';
            noSlotsMessageEl.style.display = 'block';
            disableNextButton();
            return;
        }
        noSlotsMessageEl.style.display = 'none';

        slots.forEach(slot => {
            const slotEl = document.createElement('div');
            slotEl.className = 'time-slot p-2 border mb-2 rounded cursor-pointer'; // Clases básicas
            slotEl.textContent = formatTime(slot.startTime); // slot.startTime es LocalTime

            if (slot.doctorName) { // Si la API devuelve info del médico
                const doctorBadge = document.createElement('span');
                doctorBadge.className = 'time-slot-doctor-badge ms-2'; // ms-2 para margen
                doctorBadge.textContent = slot.doctorName;
                slotEl.appendChild(doctorBadge);
            }
            slotEl.dataset.time = Array.isArray(slot.startTime) ? `${String(slot.startTime[0]).padStart(2,'0')}:${String(slot.startTime[1]).padStart(2,'0')}` : slot.startTime;
            slotEl.dataset.doctorId = slot.doctorId;

            slotEl.addEventListener('click', function() {
                handleTimeSlotSelection(this);
            });
            timeSlotsGridEl.appendChild(slotEl);
        });
    }

    function handleTimeSlotSelection(slotElement) {
        document.querySelectorAll('.time-slot.selected').forEach(el => el.classList.remove('selected'));
        slotElement.classList.add('selected');
        selectedTimeSlot = slotElement.dataset.time;
        selectedDoctorForSlot = slotElement.dataset.doctorId;

        selectedTimeInput.value = selectedTimeSlot;
        selectedDoctorIdInput.value = selectedDoctorForSlot || ''; // Si no hay doctorId, envía vacío

        enableNextButton();
    }

    function formatTime(timeData) { // Recibe LocalTime (puede ser array o string "HH:MM:SS" o {hour, minute})
        let hours, minutes;
        if (Array.isArray(timeData)) { // Formato Jackson para LocalTime: [H, M, S, NANO]
            hours = timeData[0];
            minutes = timeData[1];
        } else if (typeof timeData === 'string' && timeData.includes(':')) {
            const parts = timeData.split(':');
            hours = parseInt(parts[0], 10);
            minutes = parseInt(parts[1], 10);
        } else if (typeof timeData === 'object' && timeData !== null && typeof timeData.hour !== 'undefined') { // Formato objeto
            hours = timeData.hour;
            minutes = timeData.minute;
        } else {
            console.error("Formato de hora no reconocido:", timeData);
            return "Hora Inválida";
        }

        const tempDate = new Date(2000, 0, 1, hours, minutes); // Fecha placeholder para formatear
        return dateFns.format(tempDate, 'hh:mm a', dateFnsLocale); // ej: "09:00 AM"
    }


    // --- NAVEGACIÓN DE MESES ---
    function changeMonthAndUpdateCalendar(offset) {
        currentDisplayedCalendarDate = dateFns.addMonths(currentDisplayedCalendarDate, offset);
        const yearMonthStr = dateFns.format(currentDisplayedCalendarDate, 'yyyy-MM');

        timeSlotsGridEl.innerHTML = ''; // Limpiar slots al cambiar de mes
        noSlotsMessageEl.textContent = "Selecciona una fecha con disponibilidad para ver los horarios.";
        noSlotsMessageEl.style.display = 'block';
        updateSelectedDateTitle(null);
        disableNextButton();
        selectedFullDate = null;
        selectedDateInput.value = '';
        selectedTimeSlot = null;
        selectedTimeInput.value = '';
        selectedDoctorForSlot = null;
        selectedDoctorIdInput.value = '';

        slotsLoaderEl.style.display = 'block'; // Mostrar loader para fechas
        fetch(`/medimicita/api/patient/appointments/available-dates?specialtyId=${specialtyId}&month=${yearMonthStr}`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        })
            .then(response => {
                slotsLoaderEl.style.display = 'none';
                if (!response.ok) throw new Error('Error al cargar fechas disponibles para el nuevo mes.');
                return response.json();
            })
            .then(dates => {
                availableDatesInCurrentMonthSet = new Set(dates);
                renderCalendar(); // Re-renderizar calendario con las nuevas fechas disponibles
            })
            .catch(error => {
                slotsLoaderEl.style.display = 'none';
                console.error('Error fetching available dates for new month:', error);
                calendarDaysGridEl.innerHTML = '<p class="text-danger">Error al cargar el calendario. Intente de nuevo.</p>';
            });
    }

    prevMonthBtn.addEventListener('click', () => changeMonthAndUpdateCalendar(-1));
    nextMonthBtn.addEventListener('click', () => changeMonthAndUpdateCalendar(1));

    // --- HABILITAR/DESHABILITAR BOTÓN CONTINUAR ---
    function disableNextButton() {
        nextStepBtn.disabled = true;
    }
    function enableNextButton() {
        if (selectedFullDate && selectedTimeSlot) { // Y selectedDoctorForSlot si es mandatorio
            nextStepBtn.disabled = false;
        } else {
            nextStepBtn.disabled = true;
        }
    }

    // --- VALIDACIÓN DEL FORMULARIO ANTES DE ENVIAR (OPCIONAL) ---
    if (selectSlotForm) {
        selectSlotForm.addEventListener('submit', function(event) {
            if (!selectedFullDate || !selectedTimeSlot) {
                event.preventDefault(); // Detener el envío del formulario
                alert('Por favor, seleccione una fecha y un horario disponibles.');
                // Podrías mostrar un mensaje más elegante
            }
            // Aquí también podrías re-verificar la disponibilidad del slot si quisieras
            // antes de enviar al backend, pero la verificación final la hace el backend.
        });
    }

    // --- INICIALIZAR VISTA ---
    if (typeof initialYearMonth !== 'undefined' && typeof initialAvailableDates !== 'undefined' && typeof specialtyId !== 'undefined' && typeof dateFns !== 'undefined') {
        initializeCalendar();
    } else {
        console.error("Faltan datos iniciales para el calendario (initialYearMonth, initialAvailableDates, specialtyId) o date-fns no está cargada.");
        calendarDaysGridEl.innerHTML = '<p class="text-danger">Error al inicializar el programador de citas. Por favor, regrese al paso anterior e intente de nuevo.</p>';
        disableNextButton();
    }
});