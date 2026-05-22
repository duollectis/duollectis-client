package org.duollectis.client.utils.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.duollectis.client.utils.color.Color;
import org.duollectis.client.utils.color.ColorUtils;

import java.util.List;

/** Утилиты для создания стилизованного и градиентного текста Minecraft. */
public final class TextUtil {

	private TextUtil() {}

	/**
	 * Создаёт градиентный текст, окрашивая каждый символ в свой цвет из интерполированного градиента.
	 *
	 * @param settings настройки текста (шрифт, стиль, содержимое)
	 * @param colors   опорные цвета градиента
	 * @return составной {@link MutableText} с посимвольной окраской
	 */
	public static MutableText createGradientText(TextSettings settings, Color... colors) {
		return createGradientText(settings, List.of(colors));
	}

	public static MutableText createGradientText(TextSettings settings, List<Color> colors) {
		MutableText result = Text.empty();
		String content = settings.text();
		List<Color> gradient = ColorUtils.createGradient(content.length(), colors);

		for (int i = 0; i < content.length(); i++) {
			TextSettings charSettings = settings
				.withText(String.valueOf(content.charAt(i)))
				.withColor(gradient.get(i));

			result.append(createText(charSettings));
		}

		return result;
	}

	public static MutableText createText(TextSettings settings) {
		MutableText text = Text.empty();
		text.append(Text.of(settings.text()));
		text.setStyle(Style.EMPTY
			.withColor(settings.color().getPacked())
			.withBold(settings.bold())
			.withItalic(settings.italic())
			.withUnderline(settings.underline())
			.withStrikethrough(settings.strikethrough())
			.withObfuscated(settings.obfuscated())
			.withInsertion(settings.insertion())
			.withParent(settings.parentStyle())
		);
		return text;
	}

	/** Иммутабельные настройки форматирования текстового компонента Minecraft. */
	public record TextSettings(
		String text,
		Color color,
		boolean bold,
		boolean italic,
		boolean underline,
		boolean strikethrough,
		boolean obfuscated,
		String insertion,
		Style parentStyle
	) {

		public TextSettings(String text) {
			this(
				text,
				new Color(255, 255, 255),
				false,
				false,
				false,
				false,
				false,
				"",
				Style.EMPTY
			);
		}

		public TextSettings(String text, Color color, boolean bold) {
			this(text, color, bold, false, false, false, false, "", Style.EMPTY);
		}

		public TextSettings(String text, boolean bold) {
			this(text, new Color(255, 255, 255), bold, false, false, false, false, "", Style.EMPTY);
		}

		public TextSettings(
			String text,
			Color color,
			boolean bold,
			boolean italic,
			boolean underline,
			boolean strikethrough,
			boolean obfuscated
		) {
			this(text, color, bold, italic, underline, strikethrough, obfuscated, "", Style.EMPTY);
		}

		public TextSettings withText(String newText) {
			return new TextSettings(
				newText,
				color,
				bold,
				italic,
				underline,
				strikethrough,
				obfuscated,
				insertion,
				parentStyle
			);
		}

		public TextSettings withColor(Color newColor) {
			return new TextSettings(
				text,
				newColor,
				bold,
				italic,
				underline,
				strikethrough,
				obfuscated,
				insertion,
				parentStyle
			);
		}
	}
}
