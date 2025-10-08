package com.example.flight.domain;

import java.util.List;

public class UploadResponse {
    private List<Passenger> rows;
    private List<String> errors;
	public List<Passenger> getRows() {
		return rows;
	}
	public void setRows(List<Passenger> rows) {
		this.rows = rows;
	}
	public List<String> getErrors() {
		return errors;
	}
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
    
    
}
