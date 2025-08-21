package com.ExamPort.ExamPort.Entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class ExamOption {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long Option_id;
	
	private int option_number;
	
    @Column(nullable = false)
    @JsonProperty("option") // Map 'option' from frontend JSON to this field
    private String AvailableOption;
	
    public ExamOption() {
        // Default constructor
    }

    @JsonCreator
    public ExamOption(@JsonProperty("option") String availableOption) {
        this.AvailableOption = availableOption;
    }

    public ExamOption(long option_id, int option_number, String availableOption) {
        super();
        Option_id = option_id;
        this.option_number = option_number;
        AvailableOption = availableOption;
    }



	public long getOption_id() {
		return Option_id;
	}

	public void setOption_id(long option_id) {
		Option_id = option_id;
	}

	public int getOption_number() {
		return option_number;
	}

	public void setOption_number(int option_number) {
		this.option_number = option_number;
	}

	public String getAvailableOption() {
		return AvailableOption;
	}

	public void setAvailableOption(String availableOption) {
		AvailableOption = availableOption;
	}

	@Override
	public String toString() {
		return "Option [Option_id=" + Option_id + ", option_number=" + option_number + ", AvailableOption="
				+ AvailableOption + "]";
	}
	
}
