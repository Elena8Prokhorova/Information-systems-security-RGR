package ru.devsecops.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@Controller
@RequestMapping("/")
public class MyappApplication {
    //Список песен
    private final List<String> songs = List.of(
        "Винтаж - Автоответчик",
        "Иванушки International - Билетик в кино",
        "Григорий Лепс - Я стану водопадом",
        "UP'рель - Бритвы и Ластики",
        "Сергей Лазарев - В самое сердце",
        "Стас Пьеха - Думать о ней",
        "Звери - До скорой встречи",
        "Дмитрий Колдун - Граффити",
        "Полина Гагарина - Кукушка",
        "Король и Шут - Кукла Колдуна",
        "Инь-Ян - Камикадзе"
    );

	public static void main(String[] args) {
		SpringApplication.run(MyappApplication.class, args);
	}

    //Меры по обеспечению ИБ
    @Configuration
    @EnableWebSecurity
    public static class SecurityConfig {

        //Безопасный энкодер паролей BCrypt
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable()) // Отключаем CSRF для демонстрационных API-запросов
                .headers(headers -> headers
                    //Полная защита от Кликджекинга (запрет тегов iframe)
                    .frameOptions(frame -> frame.deny())
                    .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none';"))
                )
                .requiresChannel(channel -> channel
                    //Принудительный перевод всех запросов на шифрованный канал HTTPS
                    .anyRequest().requiresSecure()
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); //Разрешаем доступ к демо-странице
            return http.build();
        }
    }


	@GetMapping
    public String index(Model model) {
        model.addAttribute("songs", songs);
        return "index";
    }

    //Оценить песню
    @PostMapping("/grade")
    @ResponseBody
    public Map<String, String> rateSong(@RequestParam String song, @RequestParam int grade, HttpSession session) {
        session.setAttribute("lastSong", song);
        session.setAttribute("lastGrade", grade);

		System.out.println("\n[ОТВЕТ СЕРВЕРА] Получена новая оценка (Сессия №" + session.getId() +
            "): Песня = '" + song + "', Оценка = " + grade + "%n");
        return Map.of("status", "success", "message", "Оценка принята сервером!");
    }

    //Запрос для "Получить отзыв"
    @GetMapping("/review")
    @ResponseBody
    public Map<String, String> getAiReview(HttpSession session) {
        String lastSong = (String) session.getAttribute("lastSong");
        Integer lastGrade = (Integer) session.getAttribute("lastGrade");
        if (lastSong == null || lastGrade == null) {
            return Map.of("review", "Ошибка: Сначала выберите песню и отправьте оценку.");
        }

        //Формирование отзыва (имитация работы ИИ)
        String moodText = "";
        if (lastGrade == 5) {
            moodText = "Потрясающий трек с невероятной энергетикой, который моментально западает в душу. Здесь идеально всё: от узнаваемого с первых секунд мотива до мощного вокального исполнения. Настоящая классика своего жанра, которую хочется бесконечно крутить на повторе и подпевать на всю громкость.";
        } else if (lastGrade == 4) {
            moodText = "Очень качественная, сильная и мелодичная композиция, которая оставляет исключительно приятные впечатления. Прекрасная аранжировка, харизматичное исполнение и классный цепляющий припев. Немного не хватает эффекта разорвавшейся бомбы, но в личный плейлист песня добавляется однозначно.";
        } else if (lastGrade == 3) {
            moodText = "Вполне добротная, но довольно стандартная для своего времени и жанра вещь. В песне есть приятные моменты и неплохой ритм, однако в ней нет той уникальной изюминки, которая заставила бы возвращаться к ней снова и снова. Хороший фоновый трек на один-два раза.";
        } else if (lastGrade == 2) {
            moodText = "Композиция получилась довольно проходной и блеклой на фоне других заметных работ артиста. Текст кажется слишком банальным, а само звучание — монотонным и немного устаревшим. Потенциал у задумки был, но слабая реализация не дает получить удовольствие от прослушивания.";
        } else {
            moodText = "Совершенно невыразительный и скучный трек, который не вызывает никаких эмоций, кроме желания поскорее его перемотать. Перегруженная аранжировка, предсказуемый мотив и отсутствие внятной динамики. Песня абсолютно затерялась во времени и не цепляет вообще ничем.";
        }

        String aiResponse = String.format(
            "ИИ-Аналитик: Вы оценили трек «%s» на %d из 5. Сформирован отзыв: %n%n%s", 
            lastSong, lastGrade, moodText
        );
        
        return Map.of("review", aiResponse);
	}
}
