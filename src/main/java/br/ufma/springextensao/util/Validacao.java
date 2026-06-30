package br.ufma.springextensao.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class Validacao {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]{2,}$");

    public static boolean isEmailValido(String email) {
        return email != null  && !email.isBlank() && EMAIL_PATTERN.matcher(email).matches();
    }

    public static LocalDate formataDataIso(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Data não pode ser nula ou em branco.");
        }
        try {
            return LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data deve seguir o padrão ISO (yyyy-MM-dd).");
        }
    }
}
