#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

layout(std140) uniform VignetteConfig {
	float Strength;
	float Softness;
	float Roundness;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
	vec4 color = texture(InSampler, texCoord);

	vec2 uv = texCoord * 2.0 - 1.0;
	uv.x *= Roundness;

	float dist = length(uv);
	float vignette = smoothstep(1.0 - Softness, 1.0 + Softness, dist * Strength);

	fragColor = vec4(color.rgb * (1.0 - vignette), color.a);
}
