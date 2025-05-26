document.addEventListener('DOMContentLoaded', function() {
    // --- Simulación de YearMonth y LocalDate si no los tienes en el scope global de JS ---
    // Esto es solo si no usas una librería como date-fns o moment, o si no quieres implementarlo
    // de forma más robusta. Para producción, usa una librería de fechas.
    const YearMonth = {
        now: () => { const d = new Date(); return { year: d.getFullYear(), monthValue: d.getMonth() + 1, month: d.getMonth() }; }, // Simplificado
        parse: (str) => { const [y, m] = str.split('-').map(Number); return { year: y, monthValue: m, month: m - 1, atDay: (d) => ({dayOfWeek: {getValue: () => new Date(y, m-1, d).getDay() === 0 ? 7 : new Date(y, m-1, d).getDay()}}), lengthOfMonth: () => new Date(y,m,0).getDate(), plusMonths: (n) => { let newD = new Date(y,m-1,1); newD.setMonth(newD.getMonth()+n); return YearMonth.parse(`${newD.getFullYear()}-${String(newD.getMonth()+1).padStart(2,'0')}`); }}; },
        of: (y,m) => YearMonth.parse(`${y}-${String(m).padStart(2,'0')}`)
    };
    const LocalDate = { // Muy simplificado
        now: () => new Date().toISOString().split('T')[0]
    };
    const calendarTitleEl = document.getElementById('calendarTitle');
    const calendarDaysGridEl = document.getElementById('calendarDaysGrid');
    const prevMonthBtn = document.getElementById('prevMonthBtn');
    const nextMonthBtn = document.getElementById('nextMonthBtn');
    const selectedDateTitleEl = document.getElementById('selectedDateTitle');
    const timeSlotsGridEl = document.getElementById('timeSlotsGrid');
    const noSlotsMessageEl = document.getElementById('noSlotsMessage');
    const slotsLoaderEl = document.getElementById('slotsLoader');

    const selectedDateInput = document.getElementById('selectedDateInput');
    const selectedTimeInput = document.getElementById('selectedTimeInput');
    const selectedDoctorIdInput = document.getElementById('selectedDoctorIdInput'); // Si usas filtro de doctor
    const nextStepBtn = document.getElementById('nextStepBtn');

    let currentDisplayedMonth = initialYearMonth ? YearMonth.parse(initialYearMonth) : YearMonth.now();
    let availableDatesInCurrentMonth = new Set(initialAvailableDates); // Usar un Set para búsqueda rápida
    let selectedDate = null;
    let selectedTimeSlot = null;

    // --- Lógica del Calendario ---
    function renderCalendar(yearMonth) {
        calendarTitleEl.textContent = `${yearMonth.month.getDisplayName(TextStyle.FULL, new Locale('es', 'ES'))} ${yearMonth.year}`;
        calendarDaysGridEl.innerHTML = '';
        noSlotsMessageEl.style.display = 'block';
        timeSlotsGridEl.innerHTML = '';
        selectedDateTitleEl.textContent = '-- de --';
        disableNextButton();

        const firstDayOfMonth = yearMonth.atDay(1).dayOfWeek.getValue(); // 1 (Mon) to 7 (Sun)
        const daysInMonth = yearMonth.lengthOfMonth();

        // Días vacíos al inicio (asumiendo que el domingo es el primer día de la semana visualmente)
        for (let i = 0; i < (firstDayOfMonth === 7 ? 0 : firstDayOfMonth) % 7; i++) { // Ajuste para que Lunes sea el inicio
            calendarDaysGridEl.insertAdjacentHTML('beforeend', `<div class="p-1 text-center border" style="width: 14.28%; height: 50px;"></div>`);
        }

        for (let day = 1; day <= daysInMonth; day++) {
            const dateStr = `${yearMonth.year}-${String(yearMonth.monthValue).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const dayEl = document.createElement('div');
            dayEl.className = 'p-1 text-center border calendar-day';
            dayEl.style.width = '14.28%';
            dayEl.style.height = '50px';
            dayEl.textContent = day;
            dayEl.dataset.date = dateStr;

            if (availableDatesInCurrentMonth.has(dateStr)) {
                dayEl.classList.add('available');
                dayEl.addEventListener('click', function() {
                    handleDateSelection(this);
                });
            } else {
                dayEl.classList.add('disabled', 'text-muted');
            }
            // Marcar día actual
            if (dateStr === LocalDate.now().toString()) {
                dayEl.classList.add('fw-bold', 'text-primary');
            }

            calendarDaysGridEl.appendChild(dayEl);
        }
    }

    function handleDateSelection(dayElement) {
        document.querySelectorAll('.calendar-day.selected').forEach(el => el.classList.remove('selected'));
        dayElement.classList.add('selected');
        selectedDate = dayElement.dataset.date;
        selectedDateInput.value = selectedDate; // Guardar en input oculto
        selectedDateTitleEl.textContent = ` ${new Date(selectedDate + 'T00:00:00').toLocaleDateString('es-ES', { day: 'numeric', month: 'long' })}`;

        timeSlotsGridEl.innerHTML = ''; // Limpiar slots anteriores
        noSlotsMessageEl.style.display = 'none';
        slotsLoaderEl.style.display = 'block'; // Mostrar loader
        disableNextButton();
        selectedTimeSlot = null;
        selectedTimeInput.value = '';
        selectedDoctorIdInput.value = '';


        // AJAX para obtener slots de hora
        fetch(`/medimicita/api/patient/appointments/available-slots?specialtyId=${specialtyId}&date=${selectedDate}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                [csrfHeaderName]: csrfToken // Incluir CSRF si el endpoint GET lo requiere (usualmente no)
            }
        })
            .then(response => {
                slotsLoaderEl.style.display = 'none';
                if (!response.ok) {
                    throw new Error('Error al cargar horarios: ' + response.statusText);
                }
                return response.json();
            })
            .then(slots => {
                renderTimeSlots(slots);
            })
            .catch(error => {
                console.error('Error fetching time slots:', error);
                timeSlotsGridEl.innerHTML = '<p class="text-danger">Error al cargar horarios. Intente de nuevo.</p>';
                noSlotsMessageEl.style.display = 'none';
            });
    }

    function renderTimeSlots(slots) {
        timeSlotsGridEl.innerHTML = '';
        if (slots.length === 0) {
            noSlotsMessageEl.textContent = 'No hay horarios disponibles para esta fecha.';
            noSlotsMessageEl.style.display = 'block';
            return;
        }
        slots.forEach(slot => {
            const slotEl = document.createElement('div');
            slotEl.className = 'time-slot';
            slotEl.textContent = formatTime(slot.startTime); // Asume que startTime es algo como "09:00"
            if (slot.doctorName) { // Si viene info del médico (búsqueda sin médico específico)
                const doctorBadge = document.createElement('span');
                doctorBadge.className = 'time-slot-doctor-badge';
                doctorBadge.textContent = slot.doctorName;
                slotEl.appendChild(doctorBadge);
            }
            slotEl.dataset.time = slot.startTime;
            slotEl.dataset.doctorId = slot.doctorId; // Guardar ID del médico del slot

            slotEl.addEventListener('click', function() {
                document.querySelectorAll('.time-slot.selected').forEach(el => el.classList.remove('selected'));
                this.classList.add('selected');
                selectedTimeSlot = this.dataset.time;
                selectedTimeInput.value = selectedTimeSlot;
                selectedDoctorIdInput.value = this.dataset.doctorId; // Guardar el ID del médico de este slot
                enableNextButton();
            });
            timeSlotsGridEl.appendChild(slotEl);
        });
    }

    function formatTime(timeArrayOrString) { // HH:MM:SS o [H,M,S]
        if (Array.isArray(timeArrayOrString)) { // Si viene de LocalTime.toString() o similar en JSON [H, M, S, NANO]
            return `${String(timeArrayOrString[0]).padStart(2,'0')}:${String(timeArrayOrString[1]).padStart(2,'0')}`;
        }
        // Si ya es un string HH:MM
        if (typeof timeArrayOrString === 'string' && timeArrayOrString.includes(':')) {
            const parts = timeArrayOrString.split(':');
            return `${parts[0]}:${parts[1]}`;
        }
        return timeArrayOrString; // Fallback
    }

    function disableNextButton() {
        nextStepBtn.disabled = true;
    }
    function enableNextButton() {
        nextStepBtn.disabled = false;
    }

    // Navegación de Meses
    function changeMonth(offset) {
        currentDisplayedMonth = currentDisplayedMonth.plusMonths(offset);
        // AJAX para obtener nuevas fechas disponibles para el nuevo mes
        slotsLoaderEl.style.display = 'block'; // Mostrar loader mientras se cargan fechas
        fetch(`/medimicita/api/patient/appointments/available-dates?specialtyId=${specialtyId}&month=${currentDisplayedMonth.year}-${String(currentDisplayedMonth.monthValue).padStart(2, '0')}`, {
            method: 'GET',
            headers: { 'Accept': 'application/json', [csrfHeaderName]: csrfToken }
        })
            .then(response => {
                slotsLoaderEl.style.display = 'none';
                if (!response.ok) throw new Error('Error al cargar fechas');
                return response.json();
            })
            .then(dates => {
                availableDatesInCurrentMonth = new Set(dates);
                renderCalendar(currentDisplayedMonth);
            })
            .catch(error => {
                console.error('Error fetching available dates:', error);
                calendarDaysGridEl.innerHTML = '<p class="text-danger">Error al cargar el calendario.</p>';
            });
    }

    prevMonthBtn.addEventListener('click', () => changeMonth(-1));
    nextMonthBtn.addEventListener('click', () => changeMonth(1));


    const TextStyle = { FULL: 'full' }; // Placeholder
    const Locale = function(lang, country) { this.lang=lang; this.country=country; }; // Placeholder
    Date.prototype.getDisplayName = function(style, locale) { return this.toLocaleString(locale.lang + '-' + locale.country, {month: 'long'}); };


    // Inicializar calendario
    if (initialYearMonth && initialAvailableDates) {
        renderCalendar(currentDisplayedMonth);
    } else {
        console.error("Datos iniciales del calendario no encontrados.");
        calendarDaysGridEl.innerHTML = '<p class="text-danger">Error al cargar datos del calendario.</p>';
    }
});