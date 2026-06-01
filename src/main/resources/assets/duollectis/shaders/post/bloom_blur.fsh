#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

layout(std140) uniform BlurConfig {
	vec2 BlurDir;
	float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
	vec2 texelSize = 1.0 / InSize;
	vec2 step = texelSize * BlurDir;

	vec4 result = vec4(0.0);
	float radius = max(Radius, 1.0);
	float totalWeight = 0.0;

	for (float i = -radius; i <= radius; i += 1.0) {
		float weight = exp(-0.5 * (i / radius) * (i / radius));
		result += texture(InSampler, texCoord + step * i) * weight;
		totalWeight += weight;
	}

	fragColor = result / totalWeight;
}
