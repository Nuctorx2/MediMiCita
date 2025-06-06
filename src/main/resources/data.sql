-- -- src/main/resources/data.sql
--
-- -- -----------------------------------------------------
-- -- Roles Iniciales
-- -- (Asumiendo que role_created_at tiene DEFAULT CURRENT_TIMESTAMP y no necesitamos especificarlo)
-- -- -----------------------------------------------------
-- INSERT INTO roles (role_name) VALUES
--     ('PACIENTE') ON CONFLICT (role_name) DO NOTHING; -- Evita error si ya existe
-- INSERT INTO roles (role_name) VALUES
--     ('MEDICO') ON CONFLICT (role_name) DO NOTHING;
-- INSERT INTO roles (role_name) VALUES
--     ('ADMINISTRADOR') ON CONFLICT (role_name) DO NOTHING;
--
-- -- -----------------------------------------------------
-- -- EPS Iniciales
-- -- (Asumiendo defaults para eps_is_active, eps_created_at, eps_updated_at)
-- -- -----------------------------------------------------
-- INSERT INTO eps (eps_name) VALUES
--     ('EPS Sura') ON CONFLICT (eps_name) DO NOTHING;
-- INSERT INTO eps (eps_name) VALUES
--     ('EPS Sanitas') ON CONFLICT (eps_name) DO NOTHING;
-- INSERT INTO eps (eps_name) VALUES
--     ('Nueva EPS') ON CONFLICT (eps_name) DO NOTHING;
--
-- -- -----------------------------------------------------
-- -- Especialidades Iniciales
-- -- (Asumiendo defaults para specialty_is_active, etc.)
-- -- -----------------------------------------------------
-- INSERT INTO specialties (specialty_name, specialty_default_duration_minutes) VALUES
--     ('Medicina General', 30) ON CONFLICT (specialty_name) DO NOTHING;
-- INSERT INTO specialties (specialty_name, specialty_default_duration_minutes) VALUES
--     ('Odontología', 40) ON CONFLICT (specialty_name) DO NOTHING;
-- INSERT INTO specialties (specialty_name, specialty_default_duration_minutes) VALUES
--     ('Ginecología', 45) ON CONFLICT (specialty_name) DO NOTHING;
--
-- -- -----------------------------------------------------
-- -- Usuario Administrador Inicial
-- -- Reemplaza el HASH con el que generaste para 'admin123'
-- -- -----------------------------------------------------
-- INSERT INTO users (user_identification_number, user_first_name, user_last_name, user_birth_date, user_phone_number, user_email, user_password_hash, role_id, user_is_active, user_requires_password_change, user_created_at, user_updated_at)
-- VALUES
--     ('0000000001', 'Admin', 'Principal', '1990-01-01', '3001234567', 'admin@medimicita.usco.edu.co', 'TU_HASH_BCRYPT_PARA_ADMIN123', (SELECT role_id FROM roles WHERE role_name = 'ADMINISTRADOR'), TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
--     ON CONFLICT (user_identification_number) DO NOTHING
-- ON CONFLICT (user_email) DO NOTHING;
--
-- -- -----------------------------------------------------
-- -- Usuario Médico de Prueba
-- -- Reemplaza el HASH con el que generaste para 'medico123'
-- -- -----------------------------------------------------
-- INSERT INTO users (user_identification_number, user_first_name, user_last_name, user_birth_date, user_phone_number, user_email, user_password_hash, role_id, user_is_active, user_requires_password_change, user_created_at, user_updated_at)
-- VALUES
--     ('1075000001', 'Carlos', 'Ramirez', '1985-05-15', '3109876543', 'carlos.ramirez@medimicita.usco.edu.co', 'TU_HASH_BCRYPT_PARA_MEDICO123', (SELECT role_id FROM roles WHERE role_name = 'MEDICO'), TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
--     ON CONFLICT (user_identification_number) DO NOTHING
-- ON CONFLICT (user_email) DO NOTHING;
--
-- -- Insertar perfil para el médico de prueba
-- -- Asegúrate que el usuario médico ya exista
-- DO $$
-- DECLARE
-- medico_user_id BIGINT;
--     med_general_specialty_id INT;
-- BEGIN
-- SELECT user_id INTO medico_user_id FROM users WHERE user_email = 'carlos.ramirez@medimicita.usco.edu.co';
-- SELECT specialty_id INTO med_general_specialty_id FROM specialties WHERE specialty_name = 'Medicina General';
--
-- IF medico_user_id IS NOT NULL AND med_general_specialty_id IS NOT NULL THEN
--         INSERT INTO doctor_profiles (user_id, specialty_id, doctor_profile_office_number, doctor_profile_created_at, doctor_profile_updated_at)
--         VALUES (medico_user_id, med_general_specialty_id, 'Consultorio 101', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
--         ON CONFLICT (user_id) DO NOTHING; -- Si ya existe un perfil para este user_id
-- END IF;
-- END $$;
--
--
-- -- -----------------------------------------------------
-- -- Usuario Paciente de Prueba
-- -- Reemplaza el HASH con el que generaste para 'paciente123'
-- -- -----------------------------------------------------
-- INSERT INTO users (user_identification_number, user_first_name,
--                    user_last_name, user_birth_date, user_phone_number,
--                    user_email, user_password_hash, role_id, eps_id,
--                    user_terms_accepted_at, user_is_active, user_requires_password_change,
--                    user_created_at, user_updated_at)
-- VALUES
--     ('1075000002', 'Ana', 'Perez',
--      '1992-08-20', '3151112233',
--      'ana.perez@example.com',
--      'TU_HASH_BCRYPT_PARA_PACIENTE123',
--      (SELECT role_id FROM roles WHERE role_name = 'PACIENTE'),
--      (SELECT eps_id FROM eps WHERE eps_name = 'EPS Sura'),
--      CURRENT_TIMESTAMP,
--      TRUE,
--      FALSE,
--      CURRENT_TIMESTAMP,
--      CURRENT_TIMESTAMP)
--     ON CONFLICT (user_identification_number) DO NOTHING
-- ON CONFLICT (user_email) DO NOTHING;
--
-- -- Dirección para el paciente de prueba
-- -- Asegúrate que el usuario paciente ya exista
-- DO $$
-- DECLARE
-- paciente_user_id BIGINT;
-- BEGIN
-- SELECT user_id INTO paciente_user_id FROM users WHERE user_email = 'ana.perez@example.com';
-- IF paciente_user_id IS NOT NULL THEN
--         INSERT INTO user_addresses (user_id, user_address_street_type, user_address_street_number, user_address_additional_info, user_address_is_current, user_address_created_at, user_address_updated_at)
--         VALUES (paciente_user_id, 'Calle', '10A', 'Barrio Centro, Casa 2', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
--         ON CONFLICT (user_id, user_address_is_current) WHERE user_address_is_current = TRUE DO NOTHING; -- Si ya existe una dirección actual para este usuario
-- END IF;
-- END $$;
--
-- -- Puedes añadir horarios para el médico si quieres probar la disponibilidad de citas
-- -- Ajusta el valor del día de la semana (0-6 para DayOfWeek.ordinal() o 1-7 si usas Integer)
-- DO $$
-- DECLARE
-- medico_user_id BIGINT;
-- BEGIN
-- SELECT user_id INTO medico_user_id FROM users WHERE user_email = 'carlos.ramirez@medimicita.usco.edu.co';
-- IF medico_user_id IS NOT NULL THEN
--         INSERT INTO doctor_schedule_templates (doctor_user_id, doctor_schedule_template_day_of_week, doctor_schedule_template_start_time, doctor_schedule_template_end_time, doctor_schedule_template_is_active, doctor_schedule_template_created_at, doctor_schedule_template_updated_at) VALUES
--         (medico_user_id, 0, '08:00:00', '12:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Lunes (ordinal 0)
--         (medico_user_id, 0, '14:00:00', '18:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Lunes tarde
--         (medico_user_id, 2, '09:00:00', '13:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)  -- Miércoles (ordinal 2)
--         ON CONFLICT (doctor_user_id, doctor_schedule_template_day_of_week, doctor_schedule_template_start_time, doctor_schedule_template_end_time) DO NOTHING;
-- END IF;
-- END $$;

-- data.sql (temporalmente simplificado)
-- INSERT INTO roles (role_name, role_created_at) VALUES ('PACIENTE', CURRENT_TIMESTAMP);