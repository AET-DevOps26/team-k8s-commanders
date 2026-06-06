{{/*
Common helpers
*/}}

{{/* Chart full name */}}
{{- define "caredesk.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "caredesk.fullname" -}}
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

{{- define "caredesk.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels */}}
{{- define "caredesk.labels" -}}
helm.sh/chart: {{ include "caredesk.chart" . }}
app.kubernetes.io/name: {{ include "caredesk.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: caredesk
{{- end -}}

{{/* Component-specific selectorLabels (pass dict with .name) */}}
{{- define "caredesk.selectorLabels" -}}
app.kubernetes.io/name: {{ include "caredesk.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end -}}

{{/* Namespace name — informational only; Helm uses --namespace */}}
{{- define "caredesk.namespace" -}}
{{- printf "%s-devops26-team-k8s-commanders" .Values.tumId -}}
{{- end -}}

{{/* Ingress host — user value if set, else default suffix */}}
{{- define "caredesk.ingressHost" -}}
{{- if .Values.ingress.host -}}
{{- .Values.ingress.host -}}
{{- else -}}
{{- printf "caredesk-%s.student.k8s.aet.cit.tum.de" .Values.tumId -}}
{{- end -}}
{{- end -}}

{{/* Public API URL (derived from ingress host; protocol depends on TLS) */}}
{{- define "caredesk.publicApiUrl" -}}
{{- if .Values.web.env.publicApiUrl -}}
{{- .Values.web.env.publicApiUrl -}}
{{- else -}}
{{- $proto := ternary "https" "http" .Values.ingress.tls.enabled -}}
{{- printf "%s://%s/api/v1" $proto (include "caredesk.ingressHost" .) -}}
{{- end -}}
{{- end -}}

{{/* Image reference. Call with: include "caredesk.image" (dict "root" $ "image" .Values.web.image) */}}
{{- define "caredesk.image" -}}
{{- $tag := default .root.Values.images.tag .image.tag -}}
{{- printf "%s/%s:%s" .root.Values.images.registry .image.repository $tag -}}
{{- end -}}

{{/* imagePullSecrets snippet — emits the YAML key only when create=true */}}
{{- define "caredesk.imagePullSecrets" -}}
{{- if .Values.images.pullSecret.create -}}
imagePullSecrets:
  - name: {{ .Values.images.pullSecret.name }}
{{- end -}}
{{- end -}}

{{/* Backend env shared by every Spring service: DB wiring + DDL strategy.
     Call with: include "caredesk.dbEnv" (dict "root" $ "host" "..." "name" "...") */}}
{{- define "caredesk.dbEnv" -}}
- name: DB_HOST
  value: {{ .host | quote }}
- name: DB_PORT
  value: "5432"
- name: DB_NAME
  value: {{ .name | quote }}
- name: DB_USER
  value: {{ .root.Values.postgres.username | quote }}
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "caredesk.fullname" .root }}-db
      key: password
- name: SPRING_JPA_HIBERNATE_DDL_AUTO
  value: {{ .root.Values.backend.ddlAuto | quote }}
{{- end -}}

{{/* JWT secret env entry (shared by auth-service and api-gateway) */}}
{{- define "caredesk.jwtEnv" -}}
- name: JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "caredesk.fullname" . }}-app
      key: JWT_SECRET
{{- end -}}
