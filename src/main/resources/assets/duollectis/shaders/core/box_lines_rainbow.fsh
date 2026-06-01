#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in float worldY;

out vec4 fragColor;

vec3 hsvToRgb(float h, float s, float v) {
	float c = v * s;
	float x = c * (1.0 - abs(mod(h * 6.0, 2.0) - 1.0));
	float m = v - c;
	vec3 rgb;

	if (h < 1.0 / 6.0) {
		rgb = vec3(c, x, 0.0);
	} else if (h < 2.0 / 6.0) {
		rgb = vec3(x, c, 0.0);
	} else if (h < 3.0 / 6.0) {
		rgb = vec3(0.0, c, x);
	} else if (h < 4.0 / 6.0) {
		rgb = vec3(0.0, x, c);
	} else if (h < 5.0 / 6.0) {
		rgb = vec3(x, 0.0, c);
	} else {
		rgb = vec3(c, 0.0, x);
	}

	return rgb + m;
}

void main() {
	float speed = 0.5;
	float hue = fract(GameTime * speed + worldY * 0.05);
	vec3 rainbow = hsvToRgb(hue, 1.0, 1.0);
	fragColor = vec4(rainbow, vertexColor.a) * ColorModulator;
}
