#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
	vec4 texColor = texture(Sampler0, texCoord0);

	if (texColor.a < 0.1) {
		discard;
	}

	fragColor = apply_fog(
		vertexColor,
		sphericalVertexDistance,
		cylindricalVertexDistance,
		FogEnvironmentalStart,
		FogEnvironmentalEnd,
		FogRenderDistanceStart,
		FogRenderDistanceEnd,
		FogColor
	);
}
