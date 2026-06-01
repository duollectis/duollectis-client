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

	// Пульс: альфа осциллирует по синусоиде, RGB берётся из vertexColor
	float pulse = 0.5 + 0.5 * sin(GameTime * 6.28318 * 2.0);
	float alpha = mix(0.2, 1.0, pulse) * Color.a;

	vertexColor = vec4(Color.rgb, alpha);
	texCoord0 = UV0;
}
