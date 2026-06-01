#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
	vec4 worldPos = ModelViewMat * vec4(Position, 1.0);
	gl_Position = ProjMat * worldPos;

	sphericalVertexDistance = fog_spherical_distance(Position);
	cylindricalVertexDistance = fog_cylindrical_distance(Position);

	// Радужный перелив: hue смещается по UV и времени
	float hue = fract(UV0.x * 0.4 + UV0.y * 0.3 + GameTime * 0.4);
	float c = 1.0 - abs(mod(hue * 6.0, 2.0) - 1.0);
	vec3 rainbow;

	if (hue < 1.0 / 6.0) {
		rainbow = vec3(1.0, c, 0.0);
	} else if (hue < 2.0 / 6.0) {
		rainbow = vec3(c, 1.0, 0.0);
	} else if (hue < 3.0 / 6.0) {
		rainbow = vec3(0.0, 1.0, c);
	} else if (hue < 4.0 / 6.0) {
		rainbow = vec3(0.0, c, 1.0);
	} else if (hue < 5.0 / 6.0) {
		rainbow = vec3(c, 0.0, 1.0);
	} else {
		rainbow = vec3(1.0, 0.0, c);
	}

	vertexColor = vec4(rainbow, Color.a);
	texCoord0 = UV0;
}
