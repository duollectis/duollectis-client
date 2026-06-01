#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

layout(std140) uniform ColorGradeConfig {
	vec3 Shadows;
	vec3 Midtones;
	vec3 Highlights;
	float Contrast;
	float Saturation;
	float Exposure;
};

in vec2 texCoord;

out vec4 fragColor;

vec3 applyContrast(vec3 color, float contrast) {
	return (color - 0.5) * (1.0 + contrast) + 0.5;
}

vec3 applySaturation(vec3 color, float saturation) {
	float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
	return mix(vec3(luma), color, 1.0 + saturation);
}

vec3 applyColorWheels(vec3 color) {
	float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
	float shadowMask = 1.0 - smoothstep(0.0, 0.5, luma);
	float highlightMask = smoothstep(0.5, 1.0, luma);
	float midtoneMask = 1.0 - shadowMask - highlightMask;

	return color
		+ Shadows * shadowMask
		+ Midtones * midtoneMask
		+ Highlights * highlightMask;
}

void main() {
	vec4 color = texture(InSampler, texCoord);

	vec3 result = color.rgb * pow(2.0, Exposure);
	result = applyContrast(result, Contrast);
	result = applySaturation(result, Saturation);
	result = applyColorWheels(result);
	result = clamp(result, 0.0, 1.0);

	fragColor = vec4(result, color.a);
}
