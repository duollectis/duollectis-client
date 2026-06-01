#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

layout(std140) uniform ChromaticConfig {
	float Strength;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
	vec2 center = vec2(0.5, 0.5);
	vec2 offset = (texCoord - center) * Strength * 0.01;

	float r = texture(InSampler, texCoord - offset).r;
	float g = texture(InSampler, texCoord).g;
	float b = texture(InSampler, texCoord + offset).b;
	float a = texture(InSampler, texCoord).a;

	fragColor = vec4(r, g, b, a);
}
