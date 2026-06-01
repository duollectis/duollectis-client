#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;

out vec4 fragColor;

void main() {
	float pulse = 0.5 + 0.5 * sin(GameTime * 8.0 * 3.14159265);
	float alpha = mix(0.2, 1.0, pulse) * vertexColor.a;
	fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
