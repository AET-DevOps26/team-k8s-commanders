{{/*
Common helpers
*/}}

{{- define "caredesk-monitoring.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "caredesk-monitoring.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "caredesk-monitoring.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels */}}
{{- define "caredesk-monitoring.labels" -}}
helm.sh/chart: {{ include "caredesk-monitoring.chart" . }}
app.kubernetes.io/name: {{ include "caredesk-monitoring.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: caredesk
{{- end -}}

{{/* Component-specific selectorLabels (pass dict with .root and .component).
     The caredesk chart's NetworkPolicies admit Prometheus by matching the
     app.kubernetes.io/component=prometheus label emitted here — keep them in sync. */}}
{{- define "caredesk-monitoring.selectorLabels" -}}
app.kubernetes.io/name: {{ include "caredesk-monitoring.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end -}}

{{/* Ingress host — user value if set, else default suffix */}}
{{- define "caredesk-monitoring.ingressHost" -}}
{{- if .Values.ingress.host -}}
{{- .Values.ingress.host -}}
{{- else -}}
{{- print "caredesk-monitoring-team-k8s-commanders.student.k8s.aet.cit.tum.de" -}}
{{- end -}}
{{- end -}}
